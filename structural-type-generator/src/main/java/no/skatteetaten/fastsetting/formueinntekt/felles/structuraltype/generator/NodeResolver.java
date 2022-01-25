package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Optional;

public interface NodeResolver {

    Optional<String> resolve(Class<?> type, String property);

    default Optional<String> resolve(Enum<?> enumeration) {
        return resolve(enumeration.getDeclaringClass(), enumeration.name());
    }
}
