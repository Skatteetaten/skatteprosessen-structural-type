package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Field;
import java.util.function.Predicate;

public class SimpleStructuralResolver implements StructuralResolver {

    private final Predicate<Field> isRequired;

    public SimpleStructuralResolver() {
        isRequired = property -> true;
    }

    public SimpleStructuralResolver(Predicate<Field> isOptional) {
        isRequired = isOptional.negate();
    }

    @Override
    public boolean isTransient(Field property) {
        return false;
    }

    @Override
    public boolean isRequired(Field property, Class<?> type) {
        return isRequired.test(property);
    }

    @Override
    public boolean isBranch(Class<?> type) {
        return !type.getTypeName().startsWith("java.")
            && !type.getTypeName().startsWith("sun.")
            && !type.getTypeName().startsWith("jdk.");
    }

    @Override
    public String getName(Field property) {
        return property.getName();
    }
}
