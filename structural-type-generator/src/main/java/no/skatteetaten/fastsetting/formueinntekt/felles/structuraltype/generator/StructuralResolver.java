package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Field;

public interface StructuralResolver {

    boolean isTransient(Field property);

    boolean isRequired(Field property, Class<?> type);

    boolean isBranch(Class<?> type);

    String getName(Field property);
}
