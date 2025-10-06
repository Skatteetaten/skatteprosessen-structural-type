package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.squareup.javapoet.ClassName;

public class DecoratingNamingStrategy implements NamingStrategy {

    private final NamingStrategy delegate;

    private final BiFunction<ClassName, Predicate<ClassName>, ClassName> decorator;

    public DecoratingNamingStrategy(NamingStrategy delegate, Function<ClassName, ClassName> decorator) {
        this.delegate = delegate;
        this.decorator = (name, used) -> decorator.apply(name);
    }

    public DecoratingNamingStrategy(
        NamingStrategy delegate,
        BiFunction<ClassName, Predicate<ClassName>, ClassName> decorator
    ) {
        this.delegate = delegate;
        this.decorator = decorator;
    }

    public static NamingStrategy withDuplicationResolution(NamingStrategy delegate) {
        return new DecoratingNamingStrategy(delegate, (name, existing) -> {
            int index = 0;
            ClassName deduplicated = name;
            while (existing.test(deduplicated)) {
                String prefix = name.simpleName(), suffix;
                if (prefix.endsWith(STRUCTURE)) {
                    prefix = prefix.substring(0, prefix.length() - STRUCTURE.length());
                    suffix = STRUCTURE;
                } else if (prefix.endsWith(PROJECTION)) {
                    prefix = prefix.substring(0, prefix.length() - PROJECTION.length());
                    suffix = PROJECTION;
                } else if (prefix.endsWith(ENUMERATION)) {
                    prefix = prefix.substring(0, prefix.length() - ENUMERATION.length());
                    suffix = ENUMERATION;
                } else if (prefix.endsWith(EXPANSION)) {
                    prefix = prefix.substring(0, prefix.length() - EXPANSION.length());
                    suffix = EXPANSION;
                } else if (prefix.endsWith(TEMPLATE)) {
                    prefix = prefix.substring(0, prefix.length() - TEMPLATE.length());
                    suffix = TEMPLATE;
                } else {
                    suffix = "";
                }
                deduplicated = ClassName.get(name.packageName(), prefix + index++ + suffix);
            }
            return deduplicated;
        });
    }

    public static NamingStrategy withReplacements(NamingStrategy delegate, Map<Pattern, String> replacements) {
        return replacements.isEmpty() ? delegate : new DecoratingNamingStrategy(delegate, name -> {
            for (Map.Entry<Pattern, String> entry : replacements.entrySet()) {
                Matcher matcher = entry.getKey().matcher(name.toString());
                if (matcher.matches()) {
                    name = ClassName.bestGuess(matcher.replaceAll(entry.getValue()));
                }
            }
            return name;
        });
    }

    @Override
    public ClassName structure(Collection<Class<?>> types, boolean enumeration, Predicate<ClassName> used) {
        return decorator.apply(delegate.structure(types, enumeration, used), used);
    }

    @Override
    public ClassName projection(ClassName structure, Class<?> type, boolean expansion, Predicate<ClassName> used) {
        return decorator.apply(delegate.projection(structure, type, expansion, used), used);
    }

    @Override
    public ClassName template(ClassName structure, Predicate<ClassName> used) {
        return decorator.apply(delegate.template(structure, used), used);
    }
}
