package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.util.AbstractList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProjectingSingletonList<E, P> extends AbstractList<E> {

    private final Supplier<P> getter;
    private final Consumer<P> setter;

    private final Function<P, E> wrap;
    private final Function<E, P> unwrap;

    public ProjectingSingletonList(
        Supplier<P> getter, Consumer<P> setter,
        Function<P, E> wrap, Function<E, P> unwrap
    ) {
        this.getter = getter;
        this.setter = setter;
        this.wrap = wrap;
        this.unwrap = unwrap;
    }

    @Override
    public E set(int index, E element) {
        if (index == 0) {
            P value = getter.get();
            setter.accept(unwrap.apply(element));
            return wrap.apply(value);
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public void add(int index, E element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index);
        } else if (index == 0 && getter.get() == null) {
            setter.accept(unwrap.apply(element));
        }
    }

    @Override
    public E remove(int index) {
        if (index == 0) {
            P value = getter.get();
            if (value != null) {
                setter.accept(null);
                return wrap.apply(value);
            } else {
                return null;
            }
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public E get(int index) {
        if (index == 0) {
            P value = getter.get();
            if (value != null) {
                return wrap.apply(value);
            }
        }
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public int size() {
        return getter.get() == null ? 0 : 1;
    }
}
