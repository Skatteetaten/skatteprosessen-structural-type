package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

class CompoundTypedLeafDescription implements CompoundDescription {

    private final Class<?> type;

    private final List<SingularDescription> singulars;

    CompoundTypedLeafDescription(Class<?> type, List<SingularDescription> singulars) {
        this.type = type;
        this.singulars = singulars;
    }

    @Override
    public List<SingularDescription> getSingulars() {
        return singulars;
    }

    @Override
    public Sort getSort() {
        return Sort.TYPED_LEAF;
    }

    @Override
    public Optional<CompoundDescription> getSuperDescription() {
        return Optional.empty();
    }

    @Override
    public List<CompoundDescription> getSubDescriptions() {
        return Collections.emptyList();
    }

    @Override
    public void accept(
        Consumer<Class<?>> onTypedLeaf,
        Consumer<Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        Consumer<Map<String, Property>> onBranch
    ) {
        onTypedLeaf.accept(type);
    }

    @Override
    public <VALUE> VALUE apply(
        Function<Class<?>, VALUE> onTypedLeaf,
        Function<Map<String, Map<Class<?>, Enum<?>>>, VALUE> onEnumeratedLeaf,
        Function<Map<String, Property>, VALUE> onBranch
    ) {
        return onTypedLeaf.apply(type);
    }

    @Override
    public void traverse(
        BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        BiPredicate<CompoundDescription, Map<String, Property>> onBranch
    ) {
        /* do nothing */
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof CompoundDescription) {
            CompoundDescription description = (CompoundDescription) other;
            return description.getSort() == Sort.TYPED_LEAF && description.getSingulars().equals(singulars);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 17 + singulars.hashCode();
    }

    @Override
    public String toString() {
        return "Singular{sort=leaf,singulars=" + singulars + "}";
    }
}
