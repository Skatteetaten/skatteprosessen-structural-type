package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

class CompoundBranchDescription implements CompoundDescription {

    private final Map<String, Property> properties;

    private final List<SingularDescription> singulars;

    private final CompoundDescription superDescription;

    private final List<CompoundDescription> subDescriptions;

    CompoundBranchDescription(
        Map<String, Property> properties,
        List<SingularDescription> singulars,
        CompoundDescription superDescription,
        List<CompoundDescription> subDescriptions
    ) {
        this.properties = properties;
        this.singulars = singulars;
        this.superDescription = superDescription;
        this.subDescriptions = subDescriptions;
    }

    @Override
    public List<SingularDescription> getSingulars() {
        return singulars;
    }

    @Override
    public Sort getSort() {
        return Sort.BRANCH;
    }

    @Override
    public Optional<CompoundDescription> getSuperDescription() {
        return Optional.ofNullable(superDescription);
    }

    @Override
    public List<CompoundDescription> getSubDescriptions() {
        return subDescriptions;
    }

    @Override
    public void accept(
        Consumer<Class<?>> onTypedLeaf,
        Consumer<Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        Consumer<Map<String, Property>> onBranch
    ) {
        onBranch.accept(properties);
    }

    @Override
    public <VALUE> VALUE apply(
        Function<Class<?>, VALUE> onTypedLeaf,
        Function<Map<String, Map<Class<?>, Enum<?>>>, VALUE> onEnumeratedLeaf,
        Function<Map<String, Property>, VALUE> onBranch
    ) {
        return onBranch.apply(properties);
    }

    @Override
    public void traverse(
        BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        BiPredicate<CompoundDescription, Map<String, Property>> onBranch
    ) {
        if (onBranch.test(this, properties)) {
            properties.values().forEach(property -> property.getDescription().traverse(onEnumeratedLeaf, onBranch));
        }
        if (superDescription != null) {
            superDescription.traverse(onEnumeratedLeaf, onBranch);
        }
        subDescriptions.forEach(subType -> subType.traverse(onEnumeratedLeaf, onBranch));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof CompoundDescription) {
            CompoundDescription description = (CompoundDescription) other;
            return description.getSort() == Sort.BRANCH && description.getSingulars().equals(singulars);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 43 + singulars.hashCode();
    }

    @Override
    public String toString() {
        return "Singular{sort=branch,singulars=" + singulars + "}";
    }
}
