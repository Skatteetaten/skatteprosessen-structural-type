package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Optional;

import com.squareup.javapoet.CodeBlock;

public class StrictTypeResolver implements TypeResolver.WithPairedMerge {

    @Override
    public Class<?> merge(Class<?> current, Class<?> next) {
        if (current != next) {
            throw new IllegalArgumentException("Cannot merge unequal types: " + current + " and " + next);
        }
        return current;
    }

    @Override
    public Optional<CodeBlock> convert(Class<?> source, Class<?> target, CodeBlock value) {
        if (source != target) {
            throw new IllegalArgumentException("Cannot merge unequal types: " + source + " and " + target);
        }
        return Optional.empty();
    }
}
