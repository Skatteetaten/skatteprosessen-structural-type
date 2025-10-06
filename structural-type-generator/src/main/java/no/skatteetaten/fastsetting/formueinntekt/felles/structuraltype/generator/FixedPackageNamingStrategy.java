package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import com.squareup.javapoet.ClassName;

public class FixedPackageNamingStrategy implements NamingStrategy {

    private final String packageName;

    public FixedPackageNamingStrategy(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used) {
        Iterator<Class<?>> iterator = types.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException();
        }
        String name = iterator.next().getSimpleName();
        while (iterator.hasNext()) {
            if (!iterator.next().getSimpleName().equals(name)) {
                throw new IllegalArgumentException();
            }
        }
        return ClassName.get(packageName, name + (enumeration ? ENUMERATION : STRUCTURE));
    }

    @Override
    public ClassName projection(ClassName structure, Class<?> type, boolean expansion, Predicate<ClassName> used) {
        if (expansion) {
            return ClassName.get(packageName, structure.simpleName() + type.getSimpleName() + EXPANSION);
        } else {
            return ClassName.get(packageName, type.getSimpleName() + PROJECTION);
        }
    }

    @Override
    public ClassName template(ClassName structure, Predicate<ClassName> used) {
        return ClassName.get(packageName, (structure.simpleName().endsWith(STRUCTURE)
            ? structure.simpleName().substring(0, structure.simpleName().length() - STRUCTURE.length())
            : structure.simpleName()) + TEMPLATE);
    }
}
