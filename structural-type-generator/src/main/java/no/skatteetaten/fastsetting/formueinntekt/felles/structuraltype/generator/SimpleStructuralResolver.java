package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SimpleStructuralResolver implements StructuralResolver {

    private final Predicate<Field> isRequired;

    public SimpleStructuralResolver() {
        isRequired = property -> true;
    }

    public SimpleStructuralResolver(Predicate<Field> isOptional) {
        isRequired = isOptional.negate();
    }

    @Override
    public Optional<Branch<?>> toBranch(Class<?> type) {
        if (type.getTypeName().startsWith("java.")
            || type.getTypeName().startsWith("sun.")
            || type.getTypeName().startsWith("jdk.")) {
            return Optional.empty();
        } else {
            return Optional.of(new Branch<Field>() {
                @Override
                public Iterable<Field> getProperties() {
                    return () -> {
                        Set<String> names = new HashSet<>();
                        return Stream.<Class<?>>iterate(type, current -> current != Object.class && current != null, Class::getSuperclass)
                            .flatMap(current -> Stream.of(current.getDeclaredFields()))
                            .filter(field -> !field.isSynthetic())
                            .filter(field -> names.add(field.getName()))
                            .iterator();
                    };
                }

                @Override
                public boolean isRequired(Field property) {
                    return isRequired.test(property);
                }

                @Override
                public String getName(Field property) {
                    return property.getName();
                }

                @Override
                public Class<?> getType(Field property) {
                    return property.getType();
                }

                @Override
                public Type getGenericType(Field property) {
                    return property.getGenericType();
                }
            });
        }
    }
}
