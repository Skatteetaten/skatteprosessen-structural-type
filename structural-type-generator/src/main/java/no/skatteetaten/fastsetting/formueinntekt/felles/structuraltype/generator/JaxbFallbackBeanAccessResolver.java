package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import com.squareup.javapoet.CodeBlock;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class JaxbFallbackBeanAccessResolver implements AccessResolver {

    private final AccessResolver delegate;

    private final Class<? extends Annotation> xmlElement;
    private final MethodHandle name;

    @SuppressWarnings("unchecked")
    private JaxbFallbackBeanAccessResolver(AccessResolver delegate, ClassLoader classLoader, String namespace) {
        this.delegate = delegate;
        try {
            xmlElement = (Class<? extends Annotation>) Class.forName(namespace + ".XmlElement", true, classLoader);
            name = MethodHandles.publicLookup().findVirtual(xmlElement, "name", MethodType.methodType(String.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
        }
    }

    public static AccessResolver ofJavax(AccessResolver delegate) {
        return ofJavax(delegate, JaxbFallbackBeanAccessResolver.class.getClassLoader());
    }

    public static AccessResolver ofJavax(AccessResolver delegate, ClassLoader classLoader) {
        return new JaxbFallbackBeanAccessResolver(delegate, classLoader, "javax.xml.bind.annotation");
    }

    public static AccessResolver ofJakarta(AccessResolver delegate) {
        return ofJakarta(delegate, JaxbFallbackBeanAccessResolver.class.getClassLoader());
    }

    public static AccessResolver ofJakarta(AccessResolver delegate, ClassLoader classLoader) {
        return new JaxbFallbackBeanAccessResolver(delegate, classLoader, "jakarta.xml.bind.annotation");
    }

    @Override
    public CodeBlock getter(Class<?> owner, Class<?> type, String property, Cardinality cardinality, CodeBlock target) {
        try {
            return delegate.getter(owner, type, property, cardinality, target);
        } catch (IllegalStateException e) {
            return resolve(owner, property).map(resolved -> {
                try {
                    return delegate.getter(owner, type, resolved, cardinality, target);
                } catch (Exception suppressed) {
                    e.addSuppressed(suppressed);
                    throw e;
                }
            }).orElseThrow(() -> e);
        }
    }

    @Override
    public CodeBlock setter(Class<?> owner, Class<?> type, String property, Cardinality cardinality, CodeBlock target, CodeBlock value) {
        try {
            return delegate.setter(owner, type, property, cardinality, target, value);
        } catch (IllegalStateException e) {
            return resolve(owner, property).map(resolved -> {
                try {
                    return delegate.setter(owner, type, resolved, cardinality, target, value);
                } catch (Exception suppressed) {
                    e.addSuppressed(suppressed);
                    throw e;
                }
            }).orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<CodeBlock> list(Class<?> owner, Class<?> type, String property) {
        try {
            return delegate.list(owner, type, property);
        } catch (IllegalStateException e) {
            return resolve(owner, property).map(resolved -> {
                try {
                    return delegate.list(owner, type, resolved);
                } catch (Exception suppressed) {
                    e.addSuppressed(suppressed);
                    throw e;
                }
            }).orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<CodeBlock> constructor(Class<?> type) {
        return delegate.constructor(type);
    }

    private Optional<String> resolve(Class<?> owner, String property) {
        // Some XJC plugins, namely the XEW plugin ignore the JAXB naming convention for getters to be named after
        // the XSD element name and not after the Java field name. This way, the field name is considered as backup.
        return Arrays.stream(owner.getDeclaredFields()).filter(field -> {
            Annotation element = field.getAnnotation(xmlElement);
            try {
                return element != null && name.invoke(element).equals(property);
            } catch (Throwable t) {
                throw new IllegalStateException("Could not read " + xmlElement + " name of " + element, t);
            }
        }).findFirst().map(Field::getName);
    }
}
