package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import java.util.*;
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

    Optional<CompoundDescription> getSuperDescription();

    List<CompoundDescription> getSubDescriptions();

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
        Function<Class<? extends Enum<?>>, Map<Enum<?>, String>> enumerator,
        Function<List<List<SingularDescription>>, ? extends Collection<List<SingularDescription>>> grouper,
        Function<List<SingularDescription>, Function<List<SingularDescription>, List<SingularDescription>>> factory,
        List<SingularDescription> singulars
    ) {
        Function<List<SingularDescription>, List<SingularDescription>> normalizer = factory.apply(singulars);
        return of(resolver, enumerator, grouper, normalizer, normalizer.apply(singulars), new HashSet<>(), new HashMap<>());
    }

    private static CompoundDescription of(
        Function<Collection<Class<?>>, Class<?>> resolver,
        Function<Class<? extends Enum<?>>, Map<Enum<?>, String>> enumerator,
        Function<List<List<SingularDescription>>, ? extends Collection<List<SingularDescription>>> grouper,
        Function<List<SingularDescription>, List<SingularDescription>> normalizer,
        List<SingularDescription> singulars,
        Set<Set<SingularDescription>> resolved,
        Map<Set<SingularDescription>, CompoundDescription> references
    ) {
        if (singulars.isEmpty()) {
            return null;
        } else if (!resolved.add(new HashSet<>(singulars))) {
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
                    for (Map.Entry<? extends Enum<?>, String> entry : enumerator.apply(type).entrySet()) {
                        Enum<?> duplicate = names.putIfAbsent(entry.getValue(), entry.getKey());
                        if (duplicate != null) {
                            throw new IllegalStateException("Duplicate name " + entry.getValue() + " for constants " + entry.getKey() + " and " + duplicate + " of " + type.getTypeName());
                        }
                        constants.merge(entry.getValue(), Collections.singletonMap(singular.getType(), entry.getKey()), (left, right) -> Stream.concat(
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
                    of(resolver, enumerator, grouper, normalizer, leafs, resolved, references),
                    Cardinality.OPTIONAL
                ));
            }
            singulars.stream().flatMap(
                singular -> singular.getProperties().keySet().stream()
            ).distinct().forEach(name -> properties.put(name, new Property(of(
                resolver, enumerator, grouper, normalizer,
                normalizer.apply(singulars.stream()
                    .flatMap(singular -> singular.toProperties(name).stream())
                    .map(SingularDescription.Property::getDescription)
                    .collect(Collectors.toList())),
                resolved, references
            ), singulars.stream().reduce(
                leafs.isEmpty() ? Cardinality.SINGLE : Cardinality.OPTIONAL,
                (cardinality, singular) -> cardinality.merge(singular.toProperties(name).stream()
                    .map(SingularDescription.Property::getCardinality)
                    .reduce(Cardinality::merge)
                    .map(result -> singular.hasProperty(name) ? result : result.merge(Cardinality.OPTIONAL))
                    .orElse(Cardinality.OPTIONAL)),
                Cardinality::merge
            ))));
            description = new CompoundBranchDescription(
                properties,
                singulars,
                of(resolver, enumerator, grouper, normalizer, normalizer.apply(singulars.stream()
                    .flatMap(singular -> singular.getSuperDescription().stream())
                    .collect(Collectors.toList())), resolved, references),
                grouper.apply(singulars.stream().map(SingularDescription::getSubDescriptions).filter(descriptions -> !descriptions.isEmpty()).collect(Collectors.toList())).stream()
                    .map(group -> of(resolver, enumerator, grouper, normalizer, normalizer.apply(group), resolved, references))
                    .collect(Collectors.toList())
            );
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
            this.description = cardinality == Cardinality.SINGLE ? description : new BoxingDescription(description);
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
