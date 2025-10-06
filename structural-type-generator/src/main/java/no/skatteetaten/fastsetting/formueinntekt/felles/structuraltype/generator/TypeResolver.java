package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import com.squareup.javapoet.CodeBlock;

public interface TypeResolver {

    Class<?> merge(Collection<Class<?>> types);

    Optional<CodeBlock> convert(Class<?> source, Class<?> target, CodeBlock value);

    interface WithPairedMerge extends TypeResolver {

        @Override
        default Class<?> merge(Collection<Class<?>> types) {
            Iterator<Class<?>> iterator = types.iterator();
            if (!iterator.hasNext()) {
                throw new IllegalArgumentException("Cannot merge type from empty type set");
            }
            Class<?> type = iterator.next();
            while (iterator.hasNext()) {
                type = merge(type, iterator.next());
            }
            return type;
        }

        Class<?> merge(Class<?> current, Class<?> next);
    }
}
