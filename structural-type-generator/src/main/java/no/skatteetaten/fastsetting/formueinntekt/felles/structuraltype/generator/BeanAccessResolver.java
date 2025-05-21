package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.squareup.javapoet.CodeBlock;

public class BeanAccessResolver implements AccessResolver {

    private final boolean booleanWrapperPrefix, nonNullableList;

    public BeanAccessResolver() {
        booleanWrapperPrefix = false;
        nonNullableList = false;
    }

    public BeanAccessResolver(boolean booleanWrapperPrefix, boolean nonNullableList) {
        this.booleanWrapperPrefix = booleanWrapperPrefix;
        this.nonNullableList = nonNullableList;
    }

    @Override
    public CodeBlock getter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target
    ) {
        try {
            return CodeBlock.builder().add(
                "$L.$N()",
                target,
                owner.getMethod((type == boolean.class || booleanWrapperPrefix && type == Boolean.class ? "is" : "get")
                    + property.substring(0, 1).toUpperCase()
                    + property.substring(1)).getName()
            ).build();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot resolve getter for " + property + " of " + owner, e);
        }
    }

    @Override
    public CodeBlock setter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target, CodeBlock value
    ) {
        try {
            return CodeBlock.builder().add(
                "$L.$N($L)",
                target,
                owner.getMethod("set"
                    + property.substring(0, 1).toUpperCase()
                    + property.substring(1), cardinality == Cardinality.LIST ? List.class : type).getName(),
                value
            ).build();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot resolve setter for " + property + " of " + owner, e);
        }
    }

    @Override
    public Optional<CodeBlock> list(Class<?> owner, Class<?> type, String property) {
        if (nonNullableList) {
            return Optional.empty();
        }
        try {
            owner.getMethod("set"
                + property.substring(0, 1).toUpperCase()
                + property.substring(1), List.class);
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
        return Optional.of(CodeBlock.builder().add("new $T<>()", ArrayList.class).build());
    }
}
