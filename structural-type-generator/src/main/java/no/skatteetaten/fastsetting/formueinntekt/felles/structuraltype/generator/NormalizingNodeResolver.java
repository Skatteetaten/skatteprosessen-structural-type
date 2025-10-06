package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.regex.Pattern;

public class NormalizingNodeResolver implements NodeResolver {

    private final NodeResolver delegate;

    private final Pattern nonAlphaNumeric = Pattern.compile("[^a-zA-Z0-9]"), underscore = Pattern.compile("([a-z])_([a-z])");

    public NormalizingNodeResolver() {
        delegate = (type, property) -> property;
    }

    public NormalizingNodeResolver(NodeResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public String resolve(Class<?> type, String property) {
        String alphaNumeric = nonAlphaNumeric.matcher(delegate.resolve(type, property)).replaceAll("_");
        String camelCase = underscore.matcher(alphaNumeric).replaceAll(matched -> matched.group(1) + matched.group(2).toUpperCase());
        return camelCase.charAt(0) >= '0' && camelCase.charAt(0) <= '9' ? "_" + camelCase : camelCase;
    }
}
