package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import com.squareup.javapoet.ClassName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class PrioritizingNamingStrategy implements NamingStrategy {

    private final NamingStrategy delegate;

    private final ToIntFunction<Class<?>> priorization;

    public PrioritizingNamingStrategy(NamingStrategy delegate, ToIntFunction<Class<?>> priorization) {
        this.delegate = delegate;
        this.priorization = priorization;
    }

    @Override
    public ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used) {
        Map<Integer, List<Class<?>>> filtered = types.stream().collect(Collectors.groupingBy(priorization::applyAsInt));
        return delegate.structure(filtered.get(filtered.keySet().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElseThrow(() -> new IllegalStateException("Expected at least one type"))), enumeration, used);
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
