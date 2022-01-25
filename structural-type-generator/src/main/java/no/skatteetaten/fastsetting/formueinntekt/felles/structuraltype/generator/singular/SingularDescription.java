package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.NodeResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.StructuralResolver;

public interface SingularDescription {

    Class<?> getType();

    boolean isLeaf();

    Map<String, Property> getProperties();

    default boolean hasProperty(String property) {
        return getProperties().containsKey(property);
    }

    static SingularDescription of(StructuralResolver structuralResolver, NodeResolver nodeResolver, BiPredicate<Class<?>, String> condition, Class<?> type) {
        return of(structuralResolver, nodeResolver, condition, type, new HashSet<>(), new HashMap<>());
    }

    static List<SingularDescription> of(
        StructuralResolver structuralResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        List<Class<?>> types
    ) {
        Set<Class<?>> resolved = new HashSet<>();
        Map<Class<?>, SingularDescription> references = new HashMap<>();
        return types.stream()
            .distinct()
            .map(type -> of(structuralResolver, nodeResolver, condition, type, resolved, references))
            .collect(Collectors.toList());
    }

    private static SingularDescription of(
        StructuralResolver structuralResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        Class<?> type,
        Set<Class<?>> resolved,
        Map<Class<?>, SingularDescription> references
    ) {
        if (!resolved.add(type)) {
            return new SingularRecursiveDescription(() -> references.get(type));
        }
        SingularDescription description;
        if (type.isEnum() || type.isPrimitive() || !structuralResolver.isBranch(type)) {
            description = new SingularLeafDescription(type);
        } else {
            Map<String, Property> properties = new LinkedHashMap<>();
            for (Field property : type.getDeclaredFields()) {
                if (property.isSynthetic() || structuralResolver.isTransient(property) || !condition.test(type, property.getName())) {
                    continue;
                }
                Class<?> target;
                Cardinality cardinality;
                if (List.class.isAssignableFrom(property.getType())) {
                    Type generic = property.getGenericType();
                    if (!(generic instanceof ParameterizedType)
                        || ((ParameterizedType) generic).getActualTypeArguments().length != 1
                        || !(((ParameterizedType) generic).getActualTypeArguments()[0] instanceof Class<?>)) {
                        throw new IllegalArgumentException("Unexpected generic type for " + property);
                    }
                    target = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                    cardinality = Cardinality.LIST;
                } else {
                    target = property.getType();
                    cardinality = target.isPrimitive() || structuralResolver.isRequired(property, target)
                        ? Cardinality.SINGLE
                        : Cardinality.OPTIONAL;
                }
                String name = structuralResolver.getName(property);
                properties.put(nodeResolver.resolve(type, name).orElse(name), new Property(
                    of(structuralResolver, nodeResolver, condition, target, resolved, references),
                    cardinality,
                    name
                ));
            }
            description = new SingularBranchDescription(type, properties);
        }
        references.put(type, description);
        return description;
    }

    class Property {

        private final SingularDescription description;

        private final Cardinality cardinality;

        private final String name;

        Property(SingularDescription description, Cardinality cardinality, String name) {
            this.description = description;
            this.cardinality = cardinality;
            this.name = name;
        }

        public SingularDescription getDescription() {
            return description;
        }

        public Cardinality getCardinality() {
            return cardinality;
        }

        public String getName() {
            return name;
        }
    }
}
