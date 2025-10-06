package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.squareup.javapoet.ClassName;

public class CommonPrefixNamingStrategy implements NamingStrategy {

    private final Pattern dot = Pattern.compile("\\.");

    private final int minimum;

    private final boolean subpackage;

    public CommonPrefixNamingStrategy() {
        minimum = 1;
        subpackage = true;
    }

    public CommonPrefixNamingStrategy(int minimum, boolean subpackage) {
        this.minimum = minimum;
        this.subpackage = subpackage;
    }

    @Override
    public ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used) {
        Iterator<Class<?>> iterator = types.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Structure name set must not be empty");
        }
        Class<?> type = iterator.next();
        if (type.getPackageName().isEmpty()) {
            throw new IllegalArgumentException("Default package not supported: " + type);
        }
        List<String> segments = Arrays.asList(dot.split(type.getPackageName()));
        String name = type.getSimpleName();
        while (segments.size() >= minimum && iterator.hasNext()) {
            type = iterator.next();
            if (type.getPackageName().isEmpty()) {
                throw new IllegalArgumentException("Default package not supported: " + type);
            }
            int retention = 0;
            for (String segment : dot.split(type.getPackageName())) {
                if (retention == segments.size()) {
                    break;
                } else if (segment.equals(segments.get(retention))) {
                    retention++;
                } else {
                    segments = segments.subList(0, retention);
                }
            }
            int length = Math.min(name.length(), type.getSimpleName().length());
            for (int index = 0; index < length; index++) {
                if (name.charAt(index) != type.getSimpleName().charAt(index)) {
                    length = index;
                    break;
                }
            }
            name = name.substring(0, length);
        }
        if (segments.size() < minimum) {
            throw new IllegalArgumentException(
                "No common package with at least " + minimum + " segments found for " + types
            );
        } else if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty for " + types);
        }
        return ClassName.get(String.join(".", segments), name + (enumeration ? ENUMERATION : STRUCTURE));
    }

    @Override
    public ClassName projection(ClassName structure, Class<?> type, boolean expansion, Predicate<ClassName> used) {
        if (subpackage
            && !type.getPackageName().isEmpty()
            && !type.getPackageName().equals(structure.packageName())
            && type.getPackageName().startsWith(structure.packageName())
        ) {
            String suffix = dot.splitAsStream(type.getPackageName().substring(structure.packageName().length() + 1))
                .map(segment -> segment.substring(0, 1).toUpperCase() + segment.substring(1))
                .collect(Collectors.joining());
            return ClassName.get(type.getPackageName(), type.getSimpleName()
                + (type.getSimpleName().endsWith(suffix) ? "" : suffix)
                + (expansion ? EXPANSION : PROJECTION));
        } else if (expansion) {
            return ClassName.get(
                subpackage
                    ? structure.packageName() + "." + EXPANSION.toLowerCase()
                    : structure.packageName(),
                structure.simpleName() + (type.isPrimitive()
                    ? type.toString().substring(0, 1).toUpperCase() + type.toString().substring(1)
                    : type.getSimpleName()) + EXPANSION
            );
        } else {
            return ClassName.get(
                subpackage
                    ? structure.packageName() + "." + PROJECTION.toLowerCase()
                    : structure.packageName(),
                type.getSimpleName() + PROJECTION
            );
        }
    }

    @Override
    public ClassName template(ClassName structure, Predicate<ClassName> used) {
        return ClassName.get(
            subpackage
                ? structure.packageName() + "." + TEMPLATE.toLowerCase()
                : structure.packageName(),
            (structure.simpleName().endsWith(STRUCTURE)
                ? structure.simpleName().substring(0, structure.simpleName().length() - STRUCTURE.length())
                : structure.simpleName()) + TEMPLATE
        );
    }
}
