package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

public class BeanPropertyStrategy implements PropertyStrategy {

    private final boolean booleanWrapperPrefix;

    public BeanPropertyStrategy() {
        booleanWrapperPrefix = false;
    }

    public BeanPropertyStrategy(boolean booleanWrapperPrefix) {
        this.booleanWrapperPrefix = booleanWrapperPrefix;
    }

    @Override
    public String property(
        ClassName structure, String property, TypeName type,
        Cardinality cardinality, PropertyGeneration definition
    ) {
        String prefix;
        switch (definition) {
        case GETTER:
            prefix = !property.isEmpty() && (type.equals(TypeName.BOOLEAN) || booleanWrapperPrefix
                && type.isBoxedPrimitive()
                && type.unbox().equals(TypeName.BOOLEAN)) ? "is" : "get";
            break;
        case ASSUME:
            prefix = "assume";
            break;
        case SETTER:
            prefix = cardinality == Cardinality.LIST ? "add" : "set";
            break;
        case OWNER:
            prefix = "has";
            break;
        case FLUENT:
            prefix = "with";
            break;
        case FACTORY:
            prefix = "define";
            break;
        case MERGE:
            prefix = "merge";
            break;
        case CLEAR:
            prefix = "clear";
            break;
        case TRIAL:
            prefix = "trial";
            break;
        default:
            throw new IllegalStateException(definition.toString());
        }
        return property.isEmpty() ? prefix : prefix + property.substring(0, 1).toUpperCase() + property.substring(1);
    }
}
