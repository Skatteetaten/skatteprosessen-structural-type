package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

class CompoundRecursiveDescription implements CompoundDescription {

    private final Supplier<CompoundDescription> reference;

    CompoundRecursiveDescription(Supplier<CompoundDescription> reference) {
        this.reference = reference;
    }

    @Override
    public List<SingularDescription> getSingulars() {
        return reference.get().getSingulars();
    }

    @Override
    public Sort getSort() {
        return reference.get().getSort();
    }

    @Override
    public Optional<CompoundDescription> getSuperDescription() {
        return reference.get().getSuperDescription();
    }

    @Override
    public List<CompoundDescription> getSubDescriptions() {
        return reference.get().getSubDescriptions();
    }

    @Override
    public void accept(
        Consumer<Class<?>> onTypedLeaf,
        Consumer<Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        Consumer<Map<String, Property>> onBranch
    ) {
        reference.get().accept(onTypedLeaf, onEnumeratedLeaf, onBranch);
    }

    @Override
    public <VALUE> VALUE apply(
        Function<Class<?>, VALUE> onTypedLeaf,
        Function<Map<String, Map<Class<?>, Enum<?>>>, VALUE> onEnumeratedLeaf,
        Function<Map<String, Property>, VALUE> onBranch
    ) {
        return reference.get().apply(onTypedLeaf, onEnumeratedLeaf, onBranch);
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
            return reference.get().equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return reference.get().hashCode();
    }

    @Override
    public String toString() {
        return reference.get().toString();
    }
}
