package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.NodeResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.StructuralResolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public interface SingularDescription {

    Class<?> getType();

    default Class<?> getBoxedType() {
        Class<?> type = getType();
        if (type == boolean.class) {
            return Boolean.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else {
            return type;
        }
    }

    boolean isLeaf();

    Map<String, Property> getProperties();

    Optional<SingularDescription> getSuperDescription();

    List<SingularDescription> getSubDescriptions();

    default boolean hasProperty(String name) {
        return getProperties().containsKey(name);
    }

    default List<Property> toProperties(String name) {
        Queue<SingularDescription> queue = new ArrayDeque<>(Set.of(this));
        Set<SingularDescription> checked = new HashSet<>(queue);
        List<Property> properties = new ArrayList<>();
        do {
            SingularDescription description = queue.remove();
            Property property = description.getProperties().get(name);
            if (property != null) {
                properties.add(property);
            }
            description.getSuperDescription().filter(checked::add).ifPresent(queue::add);
            description.getSubDescriptions().stream().filter(checked::add).forEach(queue::add);
        } while (!queue.isEmpty());
        return properties;
    }

    static List<SingularDescription> of(
        StructuralResolver structuralResolver,
        NodeResolver nodeResolver,
        BiPredicate<Class<?>, String> condition,
        List<Class<?>> types
    ) {
        Set<Class<?>> resolved = new HashSet<>();
        Map<Class<?>, SingularDescription> references = new HashMap<>();
        return types.stream().distinct()
            .map(type -> of(structuralResolver, nodeResolver, condition, type, resolved, references))
            .collect(Collectors.toList());
    }

    private static <PROPERTY> SingularDescription of(
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
        Optional<StructuralResolver.Branch<?>> candidate;
        SingularDescription description;
        if (type.isEnum() || type.isPrimitive() || (candidate = structuralResolver.toBranch(type)).isEmpty()) {
            description = new SingularLeafDescription(type);
        } else {
            @SuppressWarnings("unchecked")
            StructuralResolver.Branch<PROPERTY> branch = (StructuralResolver.Branch<PROPERTY>) candidate.get();
            Map<String, Property> properties = new LinkedHashMap<>();
            for (PROPERTY property : branch.getProperties()) {
                String name = branch.getName(property);
                if (!condition.test(type, name)) {
                    continue;
                }
                Class<?> target = branch.getType(property);
                Cardinality cardinality;
                if (List.class.isAssignableFrom(target)) {
                    Type generic = branch.getGenericType(property);
                    if (!(generic instanceof ParameterizedType)
                        || ((ParameterizedType) generic).getActualTypeArguments().length != 1
                        || !(((ParameterizedType) generic).getActualTypeArguments()[0] instanceof Class<?>)) {
                        throw new IllegalArgumentException("Unexpected generic type for " + property);
                    }
                    target = (Class<?>) ((ParameterizedType) generic).getActualTypeArguments()[0];
                    cardinality = Cardinality.LIST;
                } else if (Collection.class.isAssignableFrom(target) || Map.class.isAssignableFrom(target)) {
                    throw new IllegalArgumentException("Only list collection types are supported: " + property);
                } else {
                    cardinality = target.isPrimitive() || branch.isRequired(property)
                        ? Cardinality.SINGLE
                        : Cardinality.OPTIONAL;
                }
                properties.put(nodeResolver.resolve(type, name), new Property(
                    of(structuralResolver, nodeResolver, condition, target, resolved, references),
                    cardinality,
                    name
                ));
            }
            description = new SingularBranchDescription(
                type,
                properties,
                branch.getSuperClass()
                    .map(superType -> of(structuralResolver, nodeResolver, condition, superType, resolved, references))
                    .orElse(null),
                branch.getSubClasses().stream()
                    .map(subType -> of(structuralResolver, nodeResolver, condition, subType, resolved, references))
                    .collect(Collectors.toList())
            );
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
