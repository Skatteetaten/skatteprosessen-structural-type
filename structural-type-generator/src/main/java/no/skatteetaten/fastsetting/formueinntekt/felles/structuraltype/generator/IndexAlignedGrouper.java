package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IndexAlignedGrouper implements Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> {

    @Override
    public Collection<List<SingularDescription>> apply(List<List<SingularDescription>> values) {
        List<List<SingularDescription>> groups = IntStream.range(0, values.stream().mapToInt(List::size).max().orElse(0))
            .mapToObj(ignored -> new ArrayList<SingularDescription>())
            .collect(Collectors.toList());
        values.forEach(value -> {
            for (int index = 0; index < value.size(); index++) {
                groups.get(index).add(value.get(index));
            }
        });
        return groups;
    }
}
