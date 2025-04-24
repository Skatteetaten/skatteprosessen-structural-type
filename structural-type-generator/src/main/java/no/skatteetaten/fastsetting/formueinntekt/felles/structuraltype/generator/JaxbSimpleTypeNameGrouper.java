package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Function;

public class JaxbSimpleTypeNameGrouper implements Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> {

    private static final String DEFAULT_VALUE = "##default";

    private final Class<? extends Annotation> xmlType;

    private final MethodHandle name;

    @SuppressWarnings("unchecked")
    private JaxbSimpleTypeNameGrouper(ClassLoader classLoader, String namespace) {
        try {
            xmlType = (Class<? extends Annotation>) Class.forName(namespace + ".XmlType", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlType, "name", MethodType.methodType(String.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    public static Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> ofJavax() {
        return ofJavax(JaxbStructuralResolver.class.getClassLoader());
    }

    public static Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> ofJavax(ClassLoader classLoader) {
        return new JaxbSimpleTypeNameGrouper(classLoader, "javax.xml.bind.annotation");
    }

    public static Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> ofJakarta() {
        return ofJakarta(JaxbStructuralResolver.class.getClassLoader());
    }

    public static Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> ofJakarta(ClassLoader classLoader) {
        return new JaxbSimpleTypeNameGrouper(classLoader, "jakarta.xml.bind.annotation");
    }

    @Override
    public Collection<List<SingularDescription>> apply(List<List<SingularDescription>> values) {
        if (values.isEmpty()) {
            return values;
        }
        Map<String, List<SingularDescription>> groups = new LinkedHashMap<>();
        values.forEach(value -> {
            Map<String, SingularDescription> names = new HashMap<>();
            value.forEach(singular -> {
                Annotation annotation = singular.getType().getAnnotation(xmlType);
                String group;
                if (annotation == null) {
                    group = DEFAULT_VALUE;
                } else {
                    try {
                        group = (String) name.invoke(annotation);
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                }
                if (group.equals(DEFAULT_VALUE)) {
                    group = singular.getType().getSimpleName();
                }
                if (names.putIfAbsent(group, singular) != null) {
                    throw new IllegalStateException("Simple type name duplicated in subclass set: " + singular + " and " + names.get(group));
                }
                groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(singular);
            });
        });
        return groups.values();
    }
}
