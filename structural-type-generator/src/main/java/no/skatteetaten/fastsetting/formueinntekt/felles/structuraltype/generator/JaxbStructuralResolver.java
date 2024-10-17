package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JaxbStructuralResolver implements StructuralResolver {

    private static final String DEFAULT_VALUE = "##default";

    private final Class<? extends Annotation> xmlTransient, xmlElement, xmlType, xmlSeeAlso, xmlEnumValue;

    private final MethodHandle name, required, xmlSeeAlsoValue, xmlEnumValueValue;

    private final boolean subtyping, enumerations;

    @SuppressWarnings("unchecked")
    private JaxbStructuralResolver(ClassLoader classLoader, String namespace) {
        this.subtyping = true;
        this.enumerations = true;
        try {
            xmlTransient = (Class<? extends Annotation>) Class.forName(namespace + ".XmlTransient", true, classLoader);
            xmlElement = (Class<? extends Annotation>) Class.forName(namespace + ".XmlElement", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlElement, "name", MethodType.methodType(String.class));
            required = MethodHandles.publicLookup().findVirtual(xmlElement, "required", MethodType.methodType(boolean.class));
            xmlType = (Class<? extends Annotation>) Class.forName(namespace + ".XmlType", true, classLoader);
            xmlSeeAlso = (Class<? extends Annotation>) Class.forName(namespace + ".XmlSeeAlso", true, classLoader);
            xmlSeeAlsoValue = MethodHandles.publicLookup().findVirtual(xmlSeeAlso, "value", MethodType.methodType(Class[].class));
            xmlEnumValue = (Class<? extends Annotation>) Class.forName(namespace + ".XmlEnumValue", true, classLoader);
            xmlEnumValueValue = MethodHandles.publicLookup().findVirtual(xmlEnumValue, "value", MethodType.methodType(String.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    private JaxbStructuralResolver(
        Class<? extends Annotation> xmlTransient,
        Class<? extends Annotation> xmlElement,
        Class<? extends Annotation> xmlType,
        Class<? extends Annotation> xmlSeeAlso,
        Class<? extends Annotation> xmlEnumValue,
        MethodHandle name,
        MethodHandle required,
        MethodHandle xmlSeeAlsoValue,
        MethodHandle xmlEnumValueValue,
        boolean subtyping,
        boolean enumerations
    ) {
        this.xmlTransient = xmlTransient;
        this.xmlElement = xmlElement;
        this.xmlType = xmlType;
        this.xmlSeeAlso = xmlSeeAlso;
        this.xmlEnumValue = xmlEnumValue;
        this.name = name;
        this.required = required;
        this.xmlSeeAlsoValue = xmlSeeAlsoValue;
        this.xmlEnumValueValue = xmlEnumValueValue;
        this.subtyping = subtyping;
        this.enumerations = enumerations;
    }

    public static JaxbStructuralResolver ofJavax() {
        return ofJavax(JaxbStructuralResolver.class.getClassLoader());
    }

    public static JaxbStructuralResolver ofJavax(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "javax.xml.bind.annotation");
    }

    public static JaxbStructuralResolver ofJakarta() {
        return ofJakarta(JaxbStructuralResolver.class.getClassLoader());
    }

    public static JaxbStructuralResolver ofJakarta(ClassLoader classLoader) {
        return new JaxbStructuralResolver(classLoader, "jakarta.xml.bind.annotation");
    }

    public JaxbStructuralResolver withSubtyping(boolean subtyping) {
        return new JaxbStructuralResolver(xmlTransient, xmlElement, xmlType, xmlSeeAlso, xmlEnumValue, name, required, xmlSeeAlsoValue, xmlEnumValueValue, subtyping, enumerations);
    }

    public JaxbStructuralResolver withEnumerations(boolean enumerations) {
        return new JaxbStructuralResolver(xmlTransient, xmlElement, xmlType, xmlSeeAlso, xmlEnumValue, name, required, xmlSeeAlsoValue, xmlEnumValueValue, subtyping, enumerations);
    }

    @Override
    public Optional<Branch<?>> toBranch(Class<?> type) {
        if (type.isAnnotationPresent(xmlType)) {
            return Optional.of(new Branch<Field>() {
                @Override
                public Iterable<Field> getProperties() {
                    return () -> {
                        Set<String> names = new HashSet<>();
                        return Stream.<Class<?>>iterate(
                                type,
                                current -> current != Object.class
                                    && current != null
                                    && (!subtyping || current == type || !current.isAnnotationPresent(xmlType)),
                                Class::getSuperclass
                            )
                            .flatMap(current -> Stream.of(current.getDeclaredFields()))
                            .filter(field -> !field.isSynthetic())
                            .filter(field -> !field.isAnnotationPresent(xmlTransient))
                            .filter(field -> names.add(field.getName()))
                            .iterator();
                    };
                }

                @Override
                public boolean isRequired(Field property) {
                    Annotation element = property.getAnnotation(xmlElement);
                    try {
                        return element != null && (boolean) required.invoke(element);
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public String getName(Field property) {
                    try {
                        Annotation annotation = property.getAnnotation(xmlElement);
                        return annotation == null || name.invoke(annotation).equals(DEFAULT_VALUE)
                            ? property.getName()
                            : ((String) name.invoke(annotation));
                    } catch (Throwable t) {
                        throw new IllegalStateException(t);
                    }
                }

                @Override
                public Class<?> getType(Field property) {
                    return property.getType();
                }

                @Override
                public Type getGenericType(Field property) {
                    return property.getGenericType();
                }

                @Override
                public Optional<Class<?>> getSuperClass() {
                    if (subtyping) {
                        Class<?> superType = type.getSuperclass();
                        return superType != null && superType.isAnnotationPresent(xmlType)
                            ? Optional.of(superType)
                            : Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                }

                @Override
                public List<Class<?>> getSubClasses() {
                    if (subtyping) {
                        Annotation xmlSeeAlso = type.getAnnotation(JaxbStructuralResolver.this.xmlSeeAlso);
                        try {
                            return xmlSeeAlso == null
                                ? Collections.emptyList()
                                : Arrays.asList((Class<?>[]) xmlSeeAlsoValue.invoke(xmlSeeAlso));
                        } catch (Throwable t) {
                            throw new IllegalStateException(t);
                        }
                    } else {
                        return Collections.emptyList();
                    }
                }
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public <TYPE extends Enum<TYPE>> Map<Enum<? extends TYPE>, String> toEnumerations(Class<? extends Enum<TYPE>> type) {
        if (!enumerations) {
            return StructuralResolver.super.toEnumerations(type);
        }
        return Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(Function.identity(), constant -> {
            try {
                Field field = type.getField(constant.name());
                Annotation annotation = field.getAnnotation(xmlEnumValue);
                return annotation == null ? constant.name() : (String) xmlEnumValueValue.invoke(annotation);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to resolve enumeration " + constant + " for " + type.getTypeName(), t);
            }
        }, (left, right) -> {
            throw new IllegalStateException();
        }, LinkedHashMap::new));
    }
}
