package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

public class ProjectingList<E, P> extends AbstractList<E> {

    private final List<P> delegate;

    private final Function<P, E> wrap;
    private final Function<E, P> unwrap;

    public ProjectingList(List<P> delegate, Function<P, E> wrap, Function<E, P> unwrap) {
        this.delegate = delegate;
        this.wrap = wrap;
        this.unwrap = unwrap;
    }

    public static <E, P> List<E> of(List<P> delegate, Function<P, E> wrap, Function<E, P> unwrap) {
        return delegate == null ? new ProjectingEmptyList<>() : new ProjectingList<>(delegate, wrap, unwrap);
    }

    @Override
    public E set(int index, E element) {
        P value = unwrap.apply(element);
        return wrap.apply(value == null ? delegate.remove(index) : delegate.set(index, value));
    }

    @Override
    public void add(int index, E element) {
        P projection = unwrap.apply(element);
        if (projection != null) {
            delegate.add(index, projection);
        }
    }

    @Override
    public E remove(int index) {
        return wrap.apply(delegate.remove(index));
    }

    @Override
    public E get(int index) {
        return wrap.apply(delegate.get(index));
    }

    @Override
    public int size() {
        return delegate.size();
    }
}
