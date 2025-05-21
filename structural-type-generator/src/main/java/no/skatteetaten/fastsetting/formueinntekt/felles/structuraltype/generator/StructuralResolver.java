package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionalInterface
public interface StructuralResolver {

    Optional<Branch<?>> toBranch(Class<?> type);

    default <TYPE extends Enum<TYPE>> Map<Enum<? extends TYPE>, String> toEnumerations(Class<? extends Enum<TYPE>> type) {
        return Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(Function.identity(), Enum::name, (left, right) -> {
            throw new IllegalStateException();
        }, LinkedHashMap::new));
    }

    interface Branch<PROPERTY> {

        Iterable<PROPERTY> getProperties();

        String getName(PROPERTY property);

        Class<?> getType(PROPERTY property);

        default Type getGenericType(PROPERTY property) {
            throw new UnsupportedOperationException();
        }

        default boolean isRequired(PROPERTY property) {
            return true;
        }

        default Optional<Class<?>> getSuperClass() {
            return Optional.empty();
        }

        default List<Class<?>> getSubClasses() {
            return Collections.emptyList();
        }
    }
}
