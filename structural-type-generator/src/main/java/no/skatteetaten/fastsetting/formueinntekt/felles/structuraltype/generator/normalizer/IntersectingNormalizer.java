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

public class IntersectingNormalizer implements Function<List<SingularDescription>, List<SingularDescription>> {

    private final Map<SingularDescription, List<SingularDescription>> references;

    private IntersectingNormalizer(Map<SingularDescription, List<SingularDescription>> references) {
        this.references = references;
    }

    public static Function<List<SingularDescription>, List<SingularDescription>> of(
        List<SingularDescription> singulars
    ) {
        Map<SingularDescription, List<SingularDescription>> references = new HashMap<>();
        harvest(singulars.stream().distinct().collect(Collectors.toList()), new HashSet<>(), references);
        boolean converged;
        do {
            converged = true;
            for (Map.Entry<SingularDescription, List<SingularDescription>> entry : references.entrySet()) {
                for (List<SingularDescription> candidates : references.values()) {
                    if (candidates.contains(entry.getKey()) && !entry.getValue().containsAll(candidates)) {
                        converged = false;
                        entry.setValue(Stream.concat(
                            entry.getValue().stream(),
                            candidates.stream()
                        ).distinct().collect(Collectors.toList()));
                    }
                }
            }
        } while (!converged);
        return new IntersectingNormalizer(references);
    }

    private static void harvest(
        List<SingularDescription> singulars,
        Set<Set<SingularDescription>> resolved,
        Map<SingularDescription, List<SingularDescription>> references
    ) {
        if (!resolved.add(new HashSet<>(singulars))) {
            return;
        }
        singulars.stream().filter(singular -> !singular.isLeaf() || singular.getType().isEnum()).forEach(singular -> references.merge(
            singular,
            singulars,
            (left, right) -> Stream.concat(left.stream(), right.stream()).distinct().collect(Collectors.toList())
        ));
        singulars.stream()
            .flatMap(singular -> singular.getProperties().keySet().stream())
            .distinct()
            .forEach(property -> harvest(singulars.stream()
                .filter(singular -> singular.hasProperty(property))
                .map(singular -> singular.getProperties().get(property).getDescription())
                .distinct()
                .collect(Collectors.toList()), resolved, references));
    }

    @Override
    public List<SingularDescription> apply(List<SingularDescription> singulars) {
        boolean leaves = singulars.stream().allMatch(SingularDescription::isLeaf);
        return singulars.stream()
            .flatMap(singular -> references.getOrDefault(singular, Collections.singletonList(singular)).stream())
            .filter(singular -> !leaves || singular.isLeaf())
            .distinct()
            .collect(Collectors.toList());
    }
}
