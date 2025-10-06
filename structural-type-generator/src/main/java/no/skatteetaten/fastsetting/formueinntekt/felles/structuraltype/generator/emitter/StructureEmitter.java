package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import com.squareup.javapoet.*;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.*;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.FeatureGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.ImplementationGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.PropertyGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

import javax.lang.model.element.Modifier;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StructureEmitter implements BiConsumer<CompoundDescription, Map<String, CompoundDescription.Property>> {

    private final Set<PropertyGeneration> propertyGenerations;

    private final Set<FeatureGeneration> featureGenerations;

    private final Set<ImplementationGeneration> implementationGenerations;

    private final NameResolver nameResolver;

    private final PropertyResolver propertyResolver;

    private final Map<ClassName, List<Class<?>>> interfaces;

    private final BiConsumer<ClassName, JavaFile> consumer;

    public StructureEmitter(
        Set<PropertyGeneration> propertyGenerations,
        Set<FeatureGeneration> featureGenerations,
        Set<ImplementationGeneration> implementationGenerations,
        NameResolver nameResolver,
        PropertyResolver propertyResolver,
        Map<ClassName, List<Class<?>>> interfaces,
        BiConsumer<ClassName, JavaFile> consumer
    ) {
        this.propertyGenerations = propertyGenerations;
        this.featureGenerations = featureGenerations;
        this.implementationGenerations = implementationGenerations;
        this.nameResolver = nameResolver;
        this.propertyResolver = propertyResolver;
        this.interfaces = interfaces;
        this.consumer = consumer;
    }

    @Override
    public void accept(CompoundDescription compound, Map<String, CompoundDescription.Property> properties) {
        ClassName structure = nameResolver.structure(compound);
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(structure)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(CompoundOf.class).addMember(
                "value",
                CodeBlock.builder().add(
                    "{ " + String.join(", ", Collections.nCopies(compound.getSingulars().size(), "$T.class")) + " }",
                    compound.getSingulars().stream().map(SingularDescription::getType).toArray(Object[]::new)
                ).build()
            ).build());
        if (!compound.getSubDescriptions().isEmpty()) {
            builder.addAnnotation(AnnotationSpec.builder(SubtypedBy.class).addMember(
                "value",
                CodeBlock.builder().add(
                    "{ " + String.join(", ", Collections.nCopies(compound.getSubDescriptions().size(), "$T.class")) + " }",
                    compound.getSubDescriptions().stream().map(nameResolver::structure).toArray(Object[]::new)
                ).build()
            ).build());
        }
        compound.getSuperDescription().ifPresent(
            superCompound -> builder.addSuperinterface(nameResolver.structure(superCompound))
        );
        if (implementationGenerations.contains(ImplementationGeneration.TEMPLATE)) {
            builder.addAnnotation(AnnotationSpec.builder(TemplatedBy.class).addMember("value", CodeBlock.builder().add(
                "$T.class",
                nameResolver.template(compound)
            ).build()).build());
            if (featureGenerations.contains(FeatureGeneration.FACTORY_ON_STRUCTURE)) {
                builder.addMethod(MethodSpec.methodBuilder("ofTemplate")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(structure)
                    .addCode(CodeBlock.builder()
                        .addStatement("return new $T()", nameResolver.template(compound))
                        .build())
                    .build());
            }
        }
        if (implementationGenerations.contains(ImplementationGeneration.PROJECTION)) {
            List<ClassName> projections = nameResolver.projections(compound);
            builder.addAnnotation(AnnotationSpec.builder(DelegatedBy.class).addMember("value", CodeBlock.builder().add(
                "{ " + String.join(", ", Collections.nCopies(projections.size(), "$T.class")) + " }",
                projections.toArray(Object[]::new)
            ).build()).build());
            if (featureGenerations.contains(FeatureGeneration.FACTORY_ON_STRUCTURE)) {
                CodeBlock.Builder ofAny = CodeBlock.builder(), ofType = CodeBlock.builder();
                Iterator<CompoundDescription> it = compound.getSubDescriptions().iterator();
                if (it.hasNext()) {
                    CompoundDescription subDescription = it.next();
                    ofAny.add(
                        "return $T.ofAny(value).map($T.class::cast)",
                        nameResolver.structure(subDescription), nameResolver.structure(compound)
                    ).indent();
                    ofType.add(
                        "return $T.ofType(type).map($T.class::cast)",
                        nameResolver.structure(subDescription), nameResolver.structure(compound)
                    ).indent();
                    while (it.hasNext()) {
                        subDescription = it.next();
                        ofAny.add(
                            "\n.or(() -> $T.ofAny(value).map($T.class::cast))",
                            nameResolver.structure(subDescription), nameResolver.structure(compound)
                        );
                        ofType.add(
                            "\n.or(() -> $T.ofType(type).map($T.class::cast))",
                            nameResolver.structure(subDescription), nameResolver.structure(compound)
                        );
                    }
                    ofAny.add("\n.or(() -> {\n").indent();
                    ofType.add("\n.or(() -> {\n").indent();
                }
                ofAny.beginControlFlow("if (value == null)").addStatement(
                    "throw new $T()", NullPointerException.class
                );
                ofType.beginControlFlow("if (type == null)").addStatement(
                    "throw new $T()", NullPointerException.class
                );
                compound.getSingulars().forEach(singular -> {
                    builder.addMethod(MethodSpec.methodBuilder("of")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(structure)
                        .addParameter(singular.getType(), "value")
                        .addCode(CodeBlock.builder().addStatement(
                            "return $T.wrap(value)",
                            nameResolver.projection(compound, singular)
                        ).build())
                        .build());
                    ofAny.nextControlFlow("else if (value instanceof $T)", singular.getBoxedType()).addStatement(
                        "return $T.of(of(($T) value))", Optional.class, singular.getBoxedType()
                    );
                    if (!singular.isLeaf() && !java.lang.reflect.Modifier.isAbstract(singular.getType().getModifiers())) {
                        ofType.nextControlFlow("else if (type == $T.class)", singular.getBoxedType()).addStatement(
                            "return $T.of(new $T())", Optional.class, nameResolver.projection(compound, singular)
                        );
                    }
                });
                ofAny.nextControlFlow("else")
                    .addStatement("return $T.empty()", Optional.class)
                    .endControlFlow();
                ofType.nextControlFlow("else")
                    .addStatement("return $T.empty()", Optional.class)
                    .endControlFlow();
                if (!compound.getSubDescriptions().isEmpty()) {
                    ofAny.unindent().addStatement("})").unindent();
                    ofType.unindent().addStatement("})").unindent();
                }
                builder.addMethod(MethodSpec.methodBuilder("ofAny")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), structure))
                    .addParameter(Object.class, "value")
                    .addCode(ofAny.build())
                    .build());
                builder.addMethod(MethodSpec.methodBuilder("ofType")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), structure))
                    .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(Object.class)
                    ), "type")
                    .addCode(ofType.build())
                    .build());
            }
        }
        if (featureGenerations.contains(FeatureGeneration.COPY)) {
            if (!propertyGenerations.containsAll(EnumSet.of(PropertyGeneration.GETTER, PropertyGeneration.SETTER))) {
                throw new IllegalStateException("The copy feature requires getter and setter properties to be enabled");
            }
            if (implementationGenerations.contains(ImplementationGeneration.PROJECTION)) {
                builder.addMethod(MethodSpec.methodBuilder("copyTo")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotations(compound.getSuperDescription().isPresent()
                        ? Collections.singleton(AnnotationSpec.builder(Override.class).build())
                        : Collections.emptySet())
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Optional.class), WildcardTypeName.subtypeOf(structure)
                    ))
                    .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Class.class), WildcardTypeName.subtypeOf(TypeName.OBJECT)
                    ), "target")
                    .addCode(CodeBlock.builder().addStatement(
                        "return this.copyTo(target, new $T<>())",
                        IdentityHashMap.class
                    ).build()).build());
                CodeBlock.Builder code = CodeBlock.builder()
                    .beginControlFlow("if (target == null)")
                    .addStatement("throw new $T()", NullPointerException.class);
                compound.getSingulars().forEach(singular -> code
                    .nextControlFlow("else if (target == $T.class)", singular.getType())
                    .addStatement(
                        "return $T.of($T.copyOf(this, copies))",
                        Optional.class, nameResolver.projection(compound, singular)
                    ));
                builder.addMethod(MethodSpec.methodBuilder("copyTo")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotations(compound.getSuperDescription().isPresent()
                        ? Collections.singleton(AnnotationSpec.builder(Override.class).build())
                        : Collections.emptySet())
                    .addParameter(ParameterizedTypeName.get(
                        ClassName.get(Class.class), WildcardTypeName.subtypeOf(TypeName.OBJECT)
                    ), "target")
                    .addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "copies")
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), WildcardTypeName.subtypeOf(structure)))
                    .addCode(code.nextControlFlow("else")
                        .addStatement("return $T.empty()", Optional.class)
                        .endControlFlow()
                        .build())
                    .build());
            }
            if (implementationGenerations.contains(ImplementationGeneration.TEMPLATE)) {
                builder.addMethod(MethodSpec.methodBuilder("copyToTemplate")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotations(compound.getSuperDescription().isPresent()
                        ? Collections.singleton(AnnotationSpec.builder(Override.class).build())
                        : Collections.emptySet())
                    .returns(structure)
                    .addCode(CodeBlock.builder().addStatement(
                        "return this.copyToTemplate(new $T<>())",
                        IdentityHashMap.class
                    ).build()).build());
                builder.addMethod(MethodSpec.methodBuilder("copyToTemplate")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addAnnotations(compound.getSuperDescription().isPresent()
                        ? Collections.singleton(AnnotationSpec.builder(Override.class).build())
                        : Collections.emptySet())
                    .returns(structure)
                    .addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "copies")
                    .addCode(CodeBlock.builder().addStatement(
                        "return $T.copyOf(this, copies)",
                        nameResolver.template(compound)
                    ).build()).build());
            }
            builder.addMethod(MethodSpec.methodBuilder("copy")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotations(compound.getSuperDescription().isPresent()
                    ? Collections.singleton(AnnotationSpec.builder(Override.class).build())
                    : Collections.emptySet())
                .returns(structure)
                .build());
        }
        if (featureGenerations.contains(FeatureGeneration.READ_DELEGATE) && compound.getSuperDescription().isEmpty()) {
            builder.addMethod(MethodSpec.methodBuilder("delegate")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), WildcardTypeName.subtypeOf(Object.class)))
                .build());
            builder.addMethod(MethodSpec.methodBuilder("delegate")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addTypeVariable(TypeVariableName.get("T"))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(TypeVariableName.get("T"))), "type")
                .returns(TypeVariableName.get("T"))
                .addCode(CodeBlock.builder()
                    .addStatement(
                        "return this.delegate().filter(type::isInstance).map(type::cast).orElseThrow(() -> new $T(this + $S + type.getTypeName()))",
                        IllegalStateException.class,
                        " does not delegate to an instance of "
                    )
                    .build())
                .build());
        }
        properties.forEach((name, property) -> property.accept((cardinality, type) -> addProperty(
            builder, structure, false, name, cardinality,
            TypeName.get(type), null
        ), (cardinality, ignored) -> addProperty(
            builder, structure, false, name, cardinality,
            nameResolver.structure(property.getDescription()), null
        ), (cardinality, nested) -> addProperty(
            builder, structure, true, name, cardinality,
            nameResolver.structure(property.getDescription()), nested.get(CompoundDescription.EXPANSION)
        )));
        if (featureGenerations.contains(FeatureGeneration.HASHCODE_EQUALS) && compound.getSuperDescription().isEmpty()) {
            addHashCode(builder);
            addEquals(builder);
        }
        if (featureGenerations.contains(FeatureGeneration.TO_STRING) && compound.getSuperDescription().isEmpty()) {
            addToString(builder);
        }
        interfaces.get(structure).stream().distinct().forEach(type -> {
            if (!type.isInterface()) {
                throw new IllegalArgumentException("Expected interface but found class " + type.getTypeName());
            }
            addInterface(builder, structure, type);
        });
        consumer.accept(structure, JavaFile.builder(
            structure.packageName(),
            builder.alwaysQualify(
                compound.getSingulars().stream()
                    .filter(singular -> !singular.isLeaf())
                    .map(singular -> singular.getType().getSimpleName())
                    .distinct()
                    .toArray(String[]::new)
            ).build()
        ).skipJavaLangImports(true).build());
    }

    private void addProperty(
        TypeSpec.Builder builder, ClassName structure, boolean branch,
        String name, Cardinality cardinality, TypeName type, CompoundDescription.Property expansion
    ) {
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            builder.addMethod(propertyResolver.getter(structure, name, type, cardinality, false, false).build());
        }
        if (cardinality == Cardinality.OPTIONAL && propertyGenerations.contains(PropertyGeneration.ASSUME)) {
            if (!propertyGenerations.contains(PropertyGeneration.GETTER)) {
                throw new IllegalStateException("Cannot generate assuming getters without the getter feature enabled");
            }
            builder.addMethod(propertyResolver.assume(structure, name, type));
        }
        if (!name.isEmpty()) {
            if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                builder.addMethod(propertyResolver.setter(structure, name, type, false, false).build());
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.setter(
                        structure, name, TypeName.get(expansionType), false, false
                    ).build()), ignored -> builder.addMethod(propertyResolver.setter(
                        structure, name, nameResolver.structure(expansion.getDescription()), false, false
                    ).build()), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
                builder.addMethod(propertyResolver.trial(structure, name, type, false, false));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.trial(
                        structure, name, TypeName.get(expansionType), false, false
                    )), ignored -> builder.addMethod(propertyResolver.trial(
                        structure, name, nameResolver.structure(expansion.getDescription()), false, false
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                builder.addMethod(propertyResolver.fluent(structure, name, type, cardinality, false, false));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.fluent(
                        structure, name, TypeName.get(expansionType), cardinality, false, false
                    )), ignored -> builder.addMethod(propertyResolver.fluent(
                        structure, name, nameResolver.structure(expansion.getDescription()), cardinality, false, false
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.MERGE)) {
                builder.addMethod(propertyResolver.merge(structure, name, type, false, null));
                if (propertyGenerations.contains(PropertyGeneration.TRIAL) && propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                    builder.addMethod(MethodSpec.methodBuilder(propertyResolver.accessor(structure, name, PropertyGeneration.MERGE))
                        .returns(structure)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addParameter(type, name)
                        .addParameter(ParameterizedTypeName.get(
                            ClassName.get(BiConsumer.class), WildcardTypeName.supertypeOf(structure), WildcardTypeName.supertypeOf(RuntimeException.class)),
                            name.equals("fallback") ? "fallback0" : "fallback"
                        )
                        .addCode(CodeBlock.builder().beginControlFlow("try").addStatement(
                            "this.$N($N)",
                            propertyResolver.accessor(structure, name, PropertyGeneration.MERGE),
                            name
                        ).nextControlFlow(
                            "catch ($T $N)",
                            RuntimeException.class,
                            name.equals("exception") ? "exception0" : "exception"
                        ).addStatement(
                            "$N.accept(this, $N)",
                            name.equals("fallback") ? "fallback0" : "fallback",
                            name.equals("exception") ? "exception0" : "exception"
                        ).endControlFlow().addStatement("return this").build())
                        .build());
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.CLEAR)) {
                builder.addMethod(propertyResolver.clear(structure, name, type, cardinality));
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.FACTORY)) {
                builder.addMethod(propertyResolver.factory(structure, name, type, null, false, null));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.factory(
                        structure, name, type,
                        TypeName.get(expansionType), false, null
                    )), ignored -> builder.addMethod(propertyResolver.factory(
                        structure, name, type,
                        nameResolver.structure(expansion.getDescription()), false, null
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
                if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                    builder.addMethod(MethodSpec.methodBuilder(propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(structure)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(type)), "consumer")
                        .addCode(CodeBlock.builder()
                            .addStatement("this.$N().ifPresent(consumer)", propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                            .addStatement("return this")
                            .build())
                        .build());
                    builder.addMethod(MethodSpec.methodBuilder(propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .returns(structure)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(type)), "consumer")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class), WildcardTypeName.supertypeOf(structure)), "fallback")
                        .addCode(CodeBlock.builder()
                            .addStatement("this.$N().ifPresentOrElse(consumer, () -> fallback.accept(this))",
                                propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                            .addStatement("return this")
                            .build())
                        .build());
                    if (expansion != null) {
                        TypeName expansionType = expansion.getDescription().apply(
                            TypeName::get,
                            ignored -> nameResolver.structure(expansion.getDescription()),
                            ignored -> {
                                throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                            }
                        );
                        builder.addMethod(MethodSpec.methodBuilder(propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .returns(structure)
                            .addParameter(expansionType, "expansion")
                            .addParameter(ParameterizedTypeName.get(ClassName.get(BiConsumer.class),
                                WildcardTypeName.supertypeOf(type),
                                WildcardTypeName.supertypeOf(expansionType)), "consumer")
                            .addCode(CodeBlock.builder()
                                .addStatement(
                                    "this.$N().ifPresentOrElse(structure -> consumer.accept(structure, expansion), () -> this.$N(expansion))",
                                    propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY),
                                    propertyResolver.accessor(structure, name, PropertyGeneration.SETTER)
                                )
                                .addStatement("return this")
                                .build())
                            .build());
                        builder.addMethod(MethodSpec.methodBuilder(propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .returns(structure)
                            .addParameter(expansionType, "expansion")
                            .addParameter(ParameterizedTypeName.get(ClassName.get(BiConsumer.class),
                                WildcardTypeName.supertypeOf(type),
                                WildcardTypeName.supertypeOf(expansionType)), "consumer")
                            .addParameter(ParameterizedTypeName.get(ClassName.get(BiConsumer.class),
                                WildcardTypeName.supertypeOf(structure),
                                WildcardTypeName.supertypeOf(expansionType)), "fallback")
                            .addCode(CodeBlock.builder()
                                .add(
                                    "this.$N().ifPresentOrElse(structure -> consumer.accept(structure, expansion), () -> {\n",
                                    propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY)
                                )
                                .indent()
                                .beginControlFlow("if (!this.$N(expansion).isPresent())", propertyResolver.accessor(structure, name, PropertyGeneration.FACTORY))
                                .addStatement("fallback.accept(this, expansion)")
                                .endControlFlow()
                                .unindent()
                                .addStatement("})")
                                .addStatement("return this")
                                .build())
                            .build());
                    }
                }
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, cardinality, false));
        }
    }

    private void addHashCode(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .build());
    }

    private void addEquals(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(boolean.class)
            .addParameter(Object.class, "other")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .build());
    }

    private void addToString(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(void.class)
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .build());
    }

    private void addInterface(TypeSpec.Builder builder, ClassName structure, Class<?> type) {
        Map<String, ClassName> variables = Arrays.stream(type.getTypeParameters()).collect(Collectors.toMap(
            TypeVariable::getName,
            variable -> {
                StructureReference proxy = variable.getAnnotation(StructureReference.class);
                if (proxy == null) {
                    throw new IllegalArgumentException(type.getTypeName()
                        + " does not annotate type variable with @"
                        + StructureReference.class.getSimpleName());
                }
                ClassName target = ClassName.bestGuess(proxy.value());
                if (!interfaces.containsKey(target)) {
                    throw new IllegalArgumentException("Unknown target " + target + " for proxy on " + type);
                }
                if (!Arrays.stream(variable.getBounds()).allMatch(bound -> {
                    if (bound instanceof Class<?>) {
                        return !interfaces.get(target).contains(bound);
                    } else if (bound instanceof ParameterizedType) {
                        return !interfaces.get(target).contains(((ParameterizedType) bound).getRawType());
                    } else {
                        throw new IllegalStateException();
                    }
                })) {
                    throw new IllegalArgumentException(
                        target + " does not implement bound of " + variable.getName() + " of " + type
                    );
                }
                return target;
            }
        ));
        Arrays.stream(type.getMethods()).filter(
            method -> !java.lang.reflect.Modifier.isStatic(method.getModifiers())
        ).filter(
            method -> !method.isSynthetic()
        ).forEach(method -> builder.methodSpecs.stream().filter(
            candidate -> candidate.name.equals(method.getName())
        ).filter(
            candidate -> candidate.parameters.size() == method.getParameterCount()
        ).filter(candidate -> {
            for (int index = 0; index < method.getParameterCount(); index++) {
                TypeName target = candidate.parameters.get(index).type;
                if (target instanceof ParameterizedTypeName) {
                    target = ((ParameterizedTypeName) target).rawType;
                }
                if (!target.equals(TypeName.get(method.getParameterTypes()[index]))) {
                    return false;
                }
            }
            return true;
        }).findAny().ifPresentOrElse(override -> {
            if (!IntStream.range(-1, method.getParameterCount()).allMatch(index -> {
                Type source = index == -1 ? method.getGenericReturnType() : method.getGenericParameterTypes()[index];
                TypeName target = index == -1 ? override.returnType : override.parameters.get(index).type;
                if (source instanceof ParameterizedType) {
                    ParameterizedType parameterized = (ParameterizedType) source;
                    if (!(target instanceof ParameterizedTypeName)
                        || ((ParameterizedTypeName) target).rawType.equals(ClassName.get(parameterized.getOwnerType()))
                        || parameterized.getActualTypeArguments().length != 1) {
                        return false;
                    } else if (((ParameterizedTypeName) target).typeArguments.size() != 1) {
                        throw new IllegalStateException();
                    } else {
                        source = parameterized.getActualTypeArguments()[0];
                        target = ((ParameterizedTypeName) target).typeArguments.get(0);
                    }
                } else if (source instanceof GenericArrayType) {
                    if (!(target instanceof ArrayTypeName)) {
                        return false;
                    } else {
                        source = ((GenericArrayType) source).getGenericComponentType();
                        target = ((ArrayTypeName) target).componentType;
                    }
                } else if (source instanceof Class<?> && ((Class<?>) source).isArray()) {
                    if (!(target instanceof ArrayTypeName)) {
                        return false;
                    } else {
                        source = ((Class<?>) source).getComponentType();
                        target = ((ArrayTypeName) target).componentType;
                    }
                }
                if (source instanceof TypeVariable<?>) {
                    return variables.get(((TypeVariable<?>) source).getName()).equals(target);
                } else {
                    return TypeName.get(source).equals(target);
                }
            })) {
                throw new IllegalArgumentException("Incompatible override: " + method
                    + " does not represent signature of " + override
                    + ": All type variables must match their proxy type representation");
            }
            builder.methodSpecs.set(
                builder.methodSpecs.indexOf(override),
                override.toBuilder().addAnnotation(Override.class).build()
            );
        }, () -> {
            if (java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
                throw new IllegalArgumentException(method + " is not overridden by " + structure);
            }
        }));
        builder.addSuperinterface(ParameterizedTypeName.get(
            ClassName.get(type),
            Arrays.stream(type.getTypeParameters())
                .map(variable -> variables.get(variable.getName()))
                .toArray(ClassName[]::new)
        ));
    }
}
