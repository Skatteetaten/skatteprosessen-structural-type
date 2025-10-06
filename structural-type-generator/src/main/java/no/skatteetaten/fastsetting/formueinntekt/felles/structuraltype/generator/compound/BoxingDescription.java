package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

class BoxingDescription implements CompoundDescription {

    private static final Map<Class<?>, Class<?>> PRIMITIVES = Map.of(
        boolean.class, Boolean.class,
        byte.class, Byte.class,
        short.class, Short.class,
        char.class, Character.class,
        int.class, Integer.class,
        long.class, Long.class,
        float.class, Float.class,
        double.class, Double.class
    );

    private final CompoundDescription delegate;

    BoxingDescription(CompoundDescription delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<SingularDescription> getSingulars() {
        return delegate.getSingulars();
    }

    @Override
    public Sort getSort() {
        return delegate.getSort();
    }

    @Override
    public Optional<CompoundDescription> getSuperDescription() {
        return delegate.getSuperDescription();
    }

    @Override
    public List<CompoundDescription> getSubDescriptions() {
        return delegate.getSubDescriptions();
    }

    @Override
    public void accept(
        Consumer<Class<?>> onTypedLeaf,
        Consumer<Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        Consumer<Map<String, Property>> onBranch
    ) {
        delegate.accept(
            type -> onTypedLeaf.accept(PRIMITIVES.getOrDefault(type, type)),
            onEnumeratedLeaf,
            onBranch
        );
    }

    @Override
    public <VALUE> VALUE apply(
        Function<Class<?>, VALUE> onTypedLeaf,
        Function<Map<String, Map<Class<?>, Enum<?>>>, VALUE> onEnumeratedLeaf,
        Function<Map<String, Property>, VALUE> onBranch
    ) {
        return delegate.apply(
            type -> onTypedLeaf.apply(PRIMITIVES.getOrDefault(type, type)),
            onEnumeratedLeaf,
            onBranch
        );
    }

    @Override
    public void traverse(
        BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> onEnumeratedLeaf,
        BiPredicate<CompoundDescription, Map<String, Property>> onBranch
    ) {
        delegate.traverse(onEnumeratedLeaf, onBranch);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof CompoundDescription) {
            return delegate.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
