package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.util.AbstractList;

public class ProjectingEmptyList<E> extends AbstractList<E> {

    @Override
    public E get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index);
        }
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public E set(int index, E element) {
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public void add(int index, E element) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index);
        }
    }

    @Override
    public E remove(int index) {
        throw new IndexOutOfBoundsException(index);
    }
}
