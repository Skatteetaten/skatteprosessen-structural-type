package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public class KeyNormalizer<KEY> implements Function<List<SingularDescription>, List<SingularDescription>> {

    private final Map<KEY, List<SingularDescription>> keys;

    private final Function<Class<?>, KEY> resolver;

    private KeyNormalizer(Map<KEY, List<SingularDescription>> keys, Function<Class<?>, KEY> resolver) {
        this.keys = keys;
        this.resolver = resolver;
    }

    public static <KEY> Function<List<SingularDescription>, List<SingularDescription>> of(
        List<SingularDescription> singulars,
        Function<Class<?>, KEY> resolver
    ) {
        Set<SingularDescription> resolved = new HashSet<>();
        Map<KEY, List<SingularDescription>> keys = new HashMap<>();
        singulars.forEach(singular -> harvest(singular, resolver, resolved, keys));
        return new KeyNormalizer<>(keys, resolver);
    }

    private static <KEY> void harvest(
        SingularDescription singular,
        Function<Class<?>, KEY> resolver,
        Set<SingularDescription> resolved,
        Map<KEY, List<SingularDescription>> keys
    ) {
        if (!resolved.add(singular)) {
            return;
        } else if (!singular.isLeaf() || singular.getType().isEnum()) {
            keys.merge(
                resolver.apply(singular.getType()),
                Collections.singletonList(singular),
                (left, right) -> Stream.concat(left.stream(), right.stream()).distinct().collect(Collectors.toList())
            );
        }
        singular.getProperties().values().forEach(property -> harvest(property.getDescription(), resolver, resolved, keys));
        singular.getSuperDescription().ifPresent(singularSuperDescription -> harvest(singularSuperDescription, resolver, resolved, keys));
        singular.getSubDescriptions().forEach(singularSubDescription -> harvest(singularSubDescription, resolver, resolved, keys));
    }

    @Override
    public List<SingularDescription> apply(List<SingularDescription> singulars) {
        return singulars.stream().flatMap(singular -> keys.getOrDefault(
            resolver.apply(singular.getType()),
            Collections.singletonList(singular)
        ).stream()).distinct().collect(Collectors.toList());
    }
}
