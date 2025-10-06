package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

@FunctionalInterface
public interface NodeResolver {

    String resolve(Class<?> type, String property);
}
