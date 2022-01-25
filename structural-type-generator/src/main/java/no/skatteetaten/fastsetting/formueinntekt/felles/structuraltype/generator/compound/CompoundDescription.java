package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;

public interface CompoundDescription {

    String EXPANSION = "";

    List<SingularDescription> getSingulars();

    Sort getSort();

    void accept(
        Consumer<Class<?>> onTypedLeaf,
        Consumer<Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        Consumer<Map<String, Property>> onBranch
    );

    <VALUE> VALUE apply(
        Function<Class<?>, VALUE> onTypedLeaf,
        Function<Map<String, Map<Class<?>, Enum<?>>>, VALUE> onEnumeratedLeaf,
        Function<Map<String, Property>, VALUE> onBranch
    );

    void traverse(
        BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        BiPredicate<CompoundDescription, Map<String, Property>> onBranch
    );

    static CompoundDescription of(
        Function<Collection<Class<?>>, Class<?>> resolver,
        Function<Enum<?>, String> enumerator,
        Function<List<SingularDescription>, Function<List<SingularDescription>, List<SingularDescription>>> factory,
        List<SingularDescription> singulars
    ) {
        Function<List<SingularDescription>, List<SingularDescription>> normalizer = factory.apply(singulars);
        return of(resolver, enumerator, normalizer, normalizer.apply(singulars), new HashSet<>(), new HashMap<>());
    }

    private static CompoundDescription of(
        Function<Collection<Class<?>>, Class<?>> resolver,
        Function<Enum<?>, String> enumerator,
        Function<List<SingularDescription>, List<SingularDescription>> normalizer,
        List<SingularDescription> singulars,
        Set<Set<SingularDescription>> resolved,
        Map<Set<SingularDescription>, CompoundDescription> references
    ) {
        if (!resolved.add(new HashSet<>(singulars))) {
            return new CompoundRecursiveDescription(() -> references.get(new HashSet<>(singulars)));
        }
        CompoundDescription description;
        if (singulars.stream().allMatch(SingularDescription::isLeaf)) {
            if (singulars.stream().map(SingularDescription::getType).allMatch(Class::isEnum)) {
                Map<String, Map<Class<?>, Enum<?>>> constants = new LinkedHashMap<>();
                for (SingularDescription singular : singulars) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) singular.getType();
                    Map<String, Enum<?>> names = new HashMap<>();
                    for (Enum<?> constant : type.getEnumConstants()) {
                        String name = enumerator.apply(constant);
                        Enum<?> duplicate = names.putIfAbsent(name, constant);
                        if (duplicate != null) {
                            throw new IllegalStateException("Duplicate name " + name + " for constants " + constant + " and " + duplicate + " of " + type.getTypeName());
                        }
                        constants.merge(name, Collections.singletonMap(type, constant), (left, right) -> Stream.concat(
                            left.entrySet().stream(),
                            right.entrySet().stream()
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                    }
                }
                description = new CompoundEnumeratedLeafDescription(constants, singulars);
            } else {
                description = new CompoundTypedLeafDescription(
                    resolver.apply(singulars.stream().map(SingularDescription::getType).collect(Collectors.toSet())),
                    singulars
                );
            }
        } else {
            Map<String, Property> properties = new LinkedHashMap<>();
            List<SingularDescription> leafs = singulars.stream()
                .filter(SingularDescription::isLeaf)
                .collect(Collectors.toList());
            if (!leafs.isEmpty()) {
                properties.put(EXPANSION, new Property(
                    of(resolver, enumerator, normalizer, leafs, resolved, references),
                    Cardinality.OPTIONAL
                ));
            }
            singulars.stream().flatMap(
                singular -> singular.getProperties().keySet().stream()
            ).distinct().forEach(name -> properties.put(name, new Property(of(
                resolver, enumerator, normalizer,
                normalizer.apply(singulars.stream()
                    .filter(singular -> singular.hasProperty(name))
                    .map(singular -> singular.getProperties().get(name).getDescription())
                    .collect(Collectors.toList())),
                resolved, references
            ), singulars.stream().reduce(
                leafs.isEmpty() ? Cardinality.SINGLE : Cardinality.OPTIONAL,
                (cardinality, singular) -> cardinality.merge(singular.hasProperty(name)
                    ? singular.getProperties().get(name).getCardinality()
                    : Cardinality.OPTIONAL),
                Cardinality::merge
            ))));
            description = new CompoundBranchDescription(properties, singulars);
        }
        references.put(new HashSet<>(singulars), description);
        return description;
    }

    enum Sort {

        TYPED_LEAF,
        ENUMERATED_LEAF,
        BRANCH;

        public boolean isLeaf() {
            return this != BRANCH;
        }
    }

    class Property {

        private final CompoundDescription description;

        private final Cardinality cardinality;

        Property(CompoundDescription description, Cardinality cardinality) {
            this.description = description;
            this.cardinality = cardinality;
        }

        public CompoundDescription getDescription() {
            return description;
        }

        public Cardinality getCardinality() {
            return cardinality;
        }

        public void accept(
            BiConsumer<Cardinality, Class<?>> onTypedLeaf,
            BiConsumer<Cardinality, Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
            BiConsumer<Cardinality, Map<String, Property>> onBranch
        ) {
            description.accept(
                type -> onTypedLeaf.accept(cardinality, type),
                constants -> onEnumeratedLeaf.accept(cardinality, constants),
                properties -> onBranch.accept(cardinality, properties)
            );
        }
    }
}
