package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public class EnumeratingNormalizer implements Function<List<SingularDescription>, List<SingularDescription>> {

    private final Map<SingularDescription, List<SingularDescription>> references;

    private EnumeratingNormalizer(Map<SingularDescription, List<SingularDescription>> references) {
        this.references = references;
    }

    public static Function<List<SingularDescription>, List<SingularDescription>> of(
        List<SingularDescription> singulars
    ) {
        Set<SingularDescription> resolved = new HashSet<>();
        Map<SingularDescription, Set<String>> enumerations = new HashMap<>();
        singulars.forEach(singular -> harvest(singular, resolved, enumerations));
        Map<SingularDescription, List<SingularDescription>> references = new HashMap<>();
        enumerations.forEach((singular, constants) -> references.put(singular, enumerations.entrySet().stream()
            .filter(entry -> entry.getValue().containsAll(constants) || constants.containsAll(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList())));
        return new EnumeratingNormalizer(references);
    }

    private static void harvest(
        SingularDescription singular,
        Set<SingularDescription> resolved,
        Map<SingularDescription, Set<String>> enumerations
    ) {
        if (!resolved.add(singular)) {
            return;
        } else if (singular.isLeaf() && singular.getType().isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) singular.getType();
            enumerations.put(
                singular,
                Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.toSet())
            );
        }
        singular.getProperties().values().forEach(property -> harvest(property.getDescription(), resolved, enumerations));
        singular.getSuperDescription().ifPresent(singularSuperDescription -> harvest(singularSuperDescription, resolved, enumerations));
        singular.getSubDescriptions().forEach(singularSubDescription -> harvest(singularSubDescription, resolved, enumerations));
    }

    @Override
    public List<SingularDescription> apply(List<SingularDescription> singulars) {
        return singulars.stream()
            .flatMap(singular -> references.getOrDefault(singular, Collections.singletonList(singular)).stream())
            .distinct()
            .collect(Collectors.toList());
    }
}
