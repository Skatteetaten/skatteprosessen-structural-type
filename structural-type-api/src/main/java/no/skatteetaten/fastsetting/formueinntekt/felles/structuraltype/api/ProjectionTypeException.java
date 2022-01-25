package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

public class ProjectionTypeException extends IllegalArgumentException {

    private final Class<?> actualType, expectedType;

    public ProjectionTypeException(Class<?> actualType, Class<?> expectedType) {
        super("Illegal projection "
            + "of type " + actualType.getTypeName() + " "
            + "where " + expectedType.getTypeName() + " was expected");
        this.actualType = actualType;
        this.expectedType = expectedType;
    }

    public Class<?> getActualType() {
        return actualType;
    }

    public Class<?> getExpectedType() {
        return expectedType;
    }
}
