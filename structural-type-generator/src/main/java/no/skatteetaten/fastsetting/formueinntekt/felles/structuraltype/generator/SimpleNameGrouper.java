package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

import java.util.*;
import java.util.function.Function;

public class SimpleNameGrouper implements Function<List<List<SingularDescription>>, Collection<List<SingularDescription>>> {

    @Override
    public Collection<List<SingularDescription>> apply(List<List<SingularDescription>> values) {
        if (values.isEmpty()) {
            return values;
        }
        Map<String, List<SingularDescription>> groups = new LinkedHashMap<>();
        values.forEach(value -> {
            Map<String, SingularDescription> names = new HashMap<>();
            value.forEach(singular -> {
                String group = singular.getType().getSimpleName();
                if (names.putIfAbsent(group, singular) != null) {
                    throw new IllegalStateException("Simple name duplicated in subclass set: " + singular + " and " + names.get(group));
                }
                groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(singular);
            });
        });
        return groups.values();
    }
}
