package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.reflect.Modifier;
import java.util.Optional;

import com.squareup.javapoet.CodeBlock;

public interface AccessResolver {

    CodeBlock getter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target
    );

    CodeBlock setter(
        Class<?> owner, Class<?> type, String property,
        Cardinality cardinality, CodeBlock target, CodeBlock value
    );

    Optional<CodeBlock> list(Class<?> owner, Class<?> type, String property);

    default Optional<CodeBlock> constructor(Class<?> type) {
        if (Modifier.isAbstract(type.getModifiers())) {
            return Optional.empty();
        }
        try {
            type.getConstructor();
            return Optional.of(CodeBlock.builder().add("new $T()", type).build());
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }
}
