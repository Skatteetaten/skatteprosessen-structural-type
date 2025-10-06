package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Function;

public class JaxbSimpleTypeKeyResolver implements Function<Class<?>, String> {

    private static final String DEFAULT_VALUE = "##default";

    private final Class<? extends Annotation> xmlType;

    private final MethodHandle name;

    @SuppressWarnings("unchecked")
    private JaxbSimpleTypeKeyResolver(ClassLoader classLoader, String namespace) {
        try {
            xmlType = (Class<? extends Annotation>) Class.forName(namespace + ".XmlType", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlType, "name", MethodType.methodType(String.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    public static Function<Class<?>, String> ofJavax() {
        return ofJavax(JaxbStructuralResolver.class.getClassLoader());
    }

    public static Function<Class<?>, String> ofJavax(ClassLoader classLoader) {
        return new JaxbSimpleTypeKeyResolver(classLoader, "javax.xml.bind.annotation");
    }

    public static Function<Class<?>, String> ofJakarta() {
        return ofJakarta(JaxbStructuralResolver.class.getClassLoader());
    }

    public static Function<Class<?>, String> ofJakarta(ClassLoader classLoader) {
        return new JaxbSimpleTypeKeyResolver(classLoader, "jakarta.xml.bind.annotation");
    }

    @Override
    public String apply(Class<?> type) {
        Annotation target = type.getAnnotation(xmlType);
        if (target != null) {
            String key;
            try {
                key = (String) name.invoke(target);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
            if (!key.equals(DEFAULT_VALUE)) {
                return key;
            }
        }
        return type.getName();
    }
}
