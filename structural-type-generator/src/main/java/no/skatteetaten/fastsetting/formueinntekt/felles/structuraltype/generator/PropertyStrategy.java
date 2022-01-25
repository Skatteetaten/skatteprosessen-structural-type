package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

@FunctionalInterface
public interface PropertyStrategy {

    String property(
        ClassName structure, String property, TypeName type,
        Cardinality cardinality, PropertyGeneration definition
    );
}
