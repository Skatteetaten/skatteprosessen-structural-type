package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.ArrayList;
import java.util.Optional;

import com.squareup.javapoet.CodeBlock;

public class FieldAccessResolver implements AccessResolver {

    @Override
    public CodeBlock getter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target
    ) {
        try {
            return CodeBlock.builder().add(
                "$L.$N",
                target,
                owner.getField(property)
            ).build();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot resolve public field for " + property + " of " + owner, e);
        }
    }

    @Override
    public CodeBlock setter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target, CodeBlock value
    ) {
        try {
            return CodeBlock.builder().add(
                "$L.$N = $L",
                target,
                owner.getField(property),
                value
            ).build();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot resolve public field for " + property + " of " + owner, e);
        }
    }

    @Override
    public Optional<CodeBlock> list(Class<?> owner, Class<?> type, String property) {
        return Optional.of(CodeBlock.builder().add("new $T<>()", ArrayList.class).build());
    }
}
