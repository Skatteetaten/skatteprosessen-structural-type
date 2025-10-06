package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Collection;
import java.util.function.Predicate;

import com.squareup.javapoet.ClassName;

public interface NamingStrategy {

    String STRUCTURE = "Structure",
        ENUMERATION = "Enumeration",
        PROJECTION = "Projection",
        TEMPLATE = "Template",
        EXPANSION = "Expansion";

    ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used);

    ClassName projection(ClassName structure, Class<?> type, boolean expansion, Predicate<ClassName> used);

    ClassName template(ClassName structure, Predicate<ClassName> used);
}
