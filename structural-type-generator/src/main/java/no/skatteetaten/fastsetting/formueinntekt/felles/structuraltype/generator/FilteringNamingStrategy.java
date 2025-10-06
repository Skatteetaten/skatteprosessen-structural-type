package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import com.squareup.javapoet.ClassName;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilteringNamingStrategy implements NamingStrategy {

    private final NamingStrategy delegate;

    private final boolean allIfEmpty;

    private final Predicate<Class<?>> filter;

    public FilteringNamingStrategy(NamingStrategy delegate, boolean allIfEmpty, Predicate<Class<?>> filter) {
        this.delegate = delegate;
        this.allIfEmpty = allIfEmpty;
        this.filter = filter;
    }

    @Override
    public ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used) {
        Collection<Class<?>> filtered = types.stream().filter(filter).collect(Collectors.toList());
        if (filtered.isEmpty()) {
            if (allIfEmpty) {
                filtered = types;
            } else {
                throw new IllegalStateException("Cannot filter all types for structure of " + types);
            }
        }
        return delegate.structure(filtered, enumeration, used);
    }

    @Override
    public ClassName projection(ClassName structure, Class<?> type, boolean expansion, Predicate<ClassName> used) {
        return delegate.projection(structure, type, expansion, used);
    }

    @Override
    public ClassName template(ClassName structure, Predicate<ClassName> used) {
        return delegate.template(structure, used);
    }
}
