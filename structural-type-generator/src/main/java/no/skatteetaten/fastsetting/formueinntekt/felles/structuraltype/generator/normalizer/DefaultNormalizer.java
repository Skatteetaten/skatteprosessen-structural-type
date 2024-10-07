package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.normalizer;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public class DefaultNormalizer implements Function<List<SingularDescription>, List<SingularDescription>> {

    @Override
    public List<SingularDescription> apply(List<SingularDescription> singulars) {
        return singulars.stream().distinct().collect(Collectors.toList());
    }
}
