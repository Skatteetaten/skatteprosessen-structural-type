package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import javax.lang.model.element.Modifier;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.DelegationOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ExpansionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectingEmptyList;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectingList;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectingSingletonList;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectionOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectionTypeException;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.AccessResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.FeatureGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.PropertyGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.TypeResolver;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public class ProjectionEmitter implements BiConsumer<CompoundDescription, Map<String, CompoundDescription.Property>> {

    private static final String PROJECTION = "projection", DELEGATE = "delegate";

    private final Set<PropertyGeneration> propertyGenerations;
    private final Set<FeatureGeneration> featureGenerations;
    private final NameResolver nameResolver;
    private final PropertyResolver propertyResolver;
    private final TypeResolver typeResolver;
    private final AccessResolver accessResolver;
    private final boolean exceptionOnEmptySetter;
    private final BiConsumer<ClassName, JavaFile> consumer;

    public ProjectionEmitter(
        Set<PropertyGeneration> propertyGenerations,
        Set<FeatureGeneration> featureGenerations,
        NameResolver nameResolver,
        PropertyResolver propertyResolver,
        TypeResolver typeResolver,
        AccessResolver accessResolver,
        boolean exceptionOnEmptySetter,
        BiConsumer<ClassName, JavaFile> consumer
    ) {
        this.propertyGenerations = propertyGenerations;
        this.featureGenerations = featureGenerations;
        this.nameResolver = nameResolver;
        this.propertyResolver = propertyResolver;
        this.typeResolver = typeResolver;
        this.accessResolver = accessResolver;
        this.exceptionOnEmptySetter = exceptionOnEmptySetter;
        this.consumer = consumer;
    }

    @Override
    public void accept(CompoundDescription compound, Map<String, CompoundDescription.Property> properties) {
        compound.getSingulars().forEach(singular -> {
            ClassName structure = nameResolver.structure(compound);
            ClassName projection = nameResolver.projection(compound, singular);
            CodeBlock.Builder wrap = CodeBlock.builder()
                .beginControlFlow("if ($N == null)", DELEGATE)
                .addStatement("return null");
            singular.getSubDescriptions().forEach(singularSubtype -> compound.getSubDescriptions().stream()
                .filter(compoundSubtype -> compoundSubtype.getSingulars().contains(singularSubtype))
                .forEach(compoundSubtype -> wrap.nextControlFlow(
                    "else if ($N instanceof $T)",
                    DELEGATE,
                    singularSubtype.getType()
                ).addStatement(
                    "return $T.wrap(($T) $N)",
                    nameResolver.projection(compoundSubtype, singularSubtype),
                    singularSubtype.getType(),
                    DELEGATE
                )));
            TypeSpec.Builder builder = TypeSpec.classBuilder(projection)
                .addSuperinterface(structure)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ProjectionOf.class).addMember("value", CodeBlock.builder().add(
                    "$T.class",
                    singular.getType()
                ).build()).build())
                .addField(FieldSpec.builder(singular.getType(), DELEGATE)
                    .addModifiers(Modifier.FINAL, Modifier.PRIVATE)
                    .build())
                .addMethod(MethodSpec.constructorBuilder()
                    .addParameter(singular.getType(), DELEGATE)
                    .addModifiers(Modifier.PROTECTED)
                    .addCode(singular.getSuperDescription().isPresent()
                        ? CodeBlock.builder().addStatement("super($N)", DELEGATE).addStatement("this.$N = $N", DELEGATE, DELEGATE).build()
                        : CodeBlock.builder().addStatement("this.$N = $N", DELEGATE, DELEGATE).build())
                    .build())
                .addMethod(MethodSpec.methodBuilder("wrap")
                    .addParameter(singular.getBoxedType(), DELEGATE)
                    .returns(projection)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addCode(wrap.nextControlFlow("else")
                        .addStatement("return new $T($N)", projection, DELEGATE)
                        .endControlFlow()
                        .build())
                    .build())
                .addMethod(MethodSpec.methodBuilder("unwrap")
                    .addParameter(structure, PROJECTION)
                    .returns(singular.getBoxedType())
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addCode(CodeBlock.builder()
                        .beginControlFlow("if ($N == null)", PROJECTION)
                        .addStatement("return null")
                        .nextControlFlow("else if ($N instanceof $T)", PROJECTION, projection)
                        .addStatement("return (($T) $N).$N", projection, PROJECTION, DELEGATE)
                        .nextControlFlow("else")
                        .addStatement(
                            "throw new $T($N.getClass(), $T.class)",
                            ProjectionTypeException.class,
                            PROJECTION,
                            projection
                        )
                        .endControlFlow()
                        .build())
                    .build());
            if (singular.isLeaf() && compound.getSort() == CompoundDescription.Sort.BRANCH) {
                CompoundDescription.Property expansion = properties.get(CompoundDescription.EXPANSION);
                CodeBlock.Builder value = CodeBlock.builder().add("value");
                MethodSpec.Builder of = MethodSpec.methodBuilder("of")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(structure);
                expansion.accept((cardinality, type) -> builder.addMethod(of.addCode(CodeBlock.builder().addStatement(
                    "return new $T($L)",
                    projection,
                    typeResolver.convert(type, singular.getType(), value.build()).orElseGet(value::build)
                ).build()).addParameter(type, "value").build()), (cardinality, enumerations) -> {
                    ClassName name = nameResolver.structure(expansion.getDescription());
                    builder.addMethod(of.addCode(CodeBlock.builder().addStatement(
                        "return new $T($T.unwrap($L, $T.class))",
                        projection,
                        name,
                        value.build(),
                        singular.getType()
                    ).build()).addParameter(name, "value").build());
                }, (cardinality, nested) -> {
                    throw new IllegalStateException();
                });
            }
            compound.getSuperDescription().ifPresent(superCompound -> singular.getSuperDescription().ifPresentOrElse(superSingular -> builder.superclass(nameResolver.projection(
                superCompound,
                superSingular
            )), () -> {
                Set<String> defined = new HashSet<>(properties.keySet());
                CompoundDescription current = superCompound;
                do {
                    ClassName name = nameResolver.structure(current);
                    current.accept(ignored -> {
                        throw new UnsupportedOperationException();
                    }, ignored -> {
                        throw new UnsupportedOperationException();
                    }, undefined -> undefined.entrySet().stream()
                        .filter(entry -> !entry.getKey().isEmpty())
                        .filter(entry -> defined.add(entry.getKey()))
                        .forEach(entry -> entry.getValue().accept((cardinality, type) -> addMissing(
                            builder, name, entry.getKey(), cardinality, TypeName.get(type), false, null
                        ), (cardinality, ignored) -> addMissing(
                            builder, name, entry.getKey(), cardinality,
                            nameResolver.structure(entry.getValue().getDescription()), false, null
                        ), (cardinality, nested) -> addMissing(
                            builder, name, entry.getKey(), cardinality,
                            nameResolver.structure(entry.getValue().getDescription()),
                            true, nested.get(CompoundDescription.EXPANSION)
                        ))));
                    current = current.getSuperDescription().orElse(null);
                } while (current != null);
            }));
            if (!singular.isLeaf()) {
                accessResolver.constructor(singular.getType()).ifPresent(
                    construction -> builder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this($L)", construction)
                        .build())
                );
            }
            if (featureGenerations.contains(FeatureGeneration.COPY)) {
                if (!propertyGenerations.containsAll(EnumSet.of(PropertyGeneration.GETTER, PropertyGeneration.SETTER))) {
                    throw new IllegalStateException("The copy feature requires getter and setter properties to be enabled");
                }
                if (singular.isLeaf()) {
                    builder
                        .addMethod(propertyResolver.copyOfExpansion(structure))
                        .addMethod(propertyResolver.copyOfExpansion(structure, projection));
                } else {
                    builder.addMethod(propertyResolver.copyOf(
                        structure,
                        compound.getSuperDescription().flatMap(superCompound -> singular.getSuperDescription().map(superSingular -> nameResolver.projection(
                            superCompound,
                            superSingular
                        ))).orElse(null),
                        accessResolver.constructor(singular.getType())
                            .map(value -> CodeBlock.builder().add("new $T($L)", projection, value).build())
                            .orElse(null)
                    )).addMethod(propertyResolver.copyOf(
                            structure,
                            properties.keySet().stream().filter(singular::hasProperty).collect(Collectors.toList()),
                            name -> properties.get(name).getDescription().getSort() != CompoundDescription.Sort.BRANCH,
                            name -> properties.get(name).getCardinality(),
                            name -> properties.get(name).getDescription().apply(
                                TypeName::get,
                                ignored -> nameResolver.structure(properties.get(name).getDescription()),
                                ignored -> nameResolver.structure(properties.get(name).getDescription())
                            ),
                            (name, copies) -> CodeBlock.builder().add(
                                "copyTo($T.class, $N).orElseThrow()",
                                singular.getProperties().get(name).getDescription().getType(),
                                copies
                            ).build()
                    ));
                    builder.addMethod(MethodSpec.methodBuilder("copy")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(structure)
                        .addAnnotation(Override.class)
                        .addCode(CodeBlock.builder().addStatement(
                            "return this.copyTo($T.class).orElseThrow()",
                            singular.getType()
                        ).build())
                        .build());
                }
            }
            if (featureGenerations.contains(FeatureGeneration.READ_DELEGATE) && singular.getSuperDescription().isEmpty()) {
                builder.addMethod(MethodSpec.methodBuilder("delegate")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), WildcardTypeName.subtypeOf(Object.class)))
                    .addAnnotation(Override.class)
                    .addCode(CodeBlock.builder().addStatement(
                        "return $T.of(this.$N)",
                        Optional.class,
                        DELEGATE
                    ).build())
                    .build());
            }
            if (singular.isLeaf()) {
                if (propertyGenerations.containsAll(EnumSet.of(PropertyGeneration.GETTER, PropertyGeneration.SETTER))) {
                    builder.addMethod(MethodSpec.methodBuilder("copy")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(structure)
                        .addAnnotation(Override.class)
                        .addCode(CodeBlock.builder().addStatement("return this").build())
                        .build());
                }
                CompoundDescription.Property expansion = properties.get(CompoundDescription.EXPANSION);
                expansion.accept((cardinality, type) -> addTypedLeafExpansion(
                    builder, structure, type, singular.getType()
                ), (cardinality, ignored) -> addEnumeratedLeafExpansion(
                    builder, structure, nameResolver.structure(expansion.getDescription())
                ), (cardinality, ignored) -> {
                    throw new IllegalStateException("Expansion property cannot be branched");
                });
                properties.entrySet().stream()
                    .filter(entry -> !entry.getKey().isEmpty())
                    .forEach(entry -> entry.getValue().accept((cardinality, type) -> addMissing(
                        builder, structure, entry.getKey(), cardinality, TypeName.get(type), false, null
                    ), (cardinality, ignored) -> addMissing(
                        builder, structure, entry.getKey(), cardinality,
                        nameResolver.structure(entry.getValue().getDescription()), false, null
                    ), (cardinality, nested) -> addMissing(
                        builder, structure, entry.getKey(), cardinality,
                        nameResolver.structure(entry.getValue().getDescription()),
                        true, nested.get(CompoundDescription.EXPANSION)
                    )));
            } else {
                builder.addAnnotation(AnnotationSpec.builder(DelegationOf.class)
                    .addMember("value", CodeBlock.builder().add("$T.class", structure).build())
                    .build());
                properties.forEach((name, property) -> {
                    if (singular.hasProperty(name)) {
                        property.accept((cardinality, type) -> addTypedLeaf(
                            builder, structure, singular.getProperties().get(name), name, cardinality,
                            singular.getType(), type
                        ), (cardinality, ignored) -> addEnumeratedLeaf(
                            builder, structure, singular.getProperties().get(name), name, cardinality,
                            singular.getType(), nameResolver.structure(property.getDescription())
                        ), (cardinality, nested) -> addBranch(
                            builder, structure, singular.getProperties().get(name), name, cardinality,
                            singular.getType(), nameResolver.structure(property.getDescription()),
                            nameResolver.projection(
                                property.getDescription(),
                                singular.getProperties().get(name).getDescription()
                            ),
                            property.getDescription(), nested.get(CompoundDescription.EXPANSION)
                        ));
                    } else {
                        property.accept((cardinality, type) -> addMissing(
                            builder, structure, name, cardinality, TypeName.get(type), false, null
                        ), (cardinality, ignored) -> addMissing(
                            builder, structure, name, cardinality, nameResolver.structure(property.getDescription()),
                            false, null
                        ), (cardinality, nested) -> addMissing(
                            builder, structure, name, cardinality, nameResolver.structure(property.getDescription()),
                            true, nested.get(CompoundDescription.EXPANSION)
                        ));
                    }
                });
            }
            if (featureGenerations.contains(FeatureGeneration.HASHCODE_EQUALS)) {
                addHashCode(builder, projection, compound.getSuperDescription().flatMap(superCompound -> singular.getSuperDescription().map(superSingular -> nameResolver.projection(
                    superCompound,
                    superSingular
                ))).orElse(null), singular, properties);
                addEquals(builder, projection, compound.getSuperDescription().flatMap(superCompound -> singular.getSuperDescription().map(superSingular -> nameResolver.projection(
                    superCompound,
                    superSingular
                ))).orElse(null), singular, properties);
            }
            if (featureGenerations.contains(FeatureGeneration.TO_STRING)) {
                addToString(builder, structure, projection, compound.getSuperDescription().flatMap(superCompound -> singular.getSuperDescription().map(superSingular -> nameResolver.projection(
                    superCompound,
                    superSingular
                ))).orElse(null), singular, properties);
            }
            consumer.accept(projection, JavaFile.builder(
                projection.packageName(), builder.build()
            ).skipJavaLangImports(true).build());
        });
    }

    private void addMissing(
        TypeSpec.Builder builder, ClassName structure,
        String name, Cardinality cardinality, TypeName type,
        boolean branch, CompoundDescription.Property expansion
    ) {
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            CodeBlock.Builder getter;
            switch (cardinality) {
            case SINGLE:
                getter = CodeBlock.builder().addStatement("return null");
                break;
            case OPTIONAL:
                getter = CodeBlock.builder().addStatement("return $T.empty()", Optional.class);
                break;
            case LIST:
                getter = propertyGenerations.contains(PropertyGeneration.SETTER)
                    ? CodeBlock.builder().addStatement("return new $T<>()", ProjectingEmptyList.class)
                    : CodeBlock.builder().addStatement("return $T.emptyList()", Collections.class);
                break;
            default:
                throw new IllegalStateException();
            }
            builder.addMethod(propertyResolver.getter(structure, name, type, cardinality, true, true)
                .addCode(getter.build())
                .build());
        }
        if (!name.isEmpty()) {
            if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                CodeBlock implementation = exceptionOnEmptySetter ? CodeBlock.builder().addStatement(
                    "throw new $T($S)", UnsupportedOperationException.class, name
                ).build() : CodeBlock.builder().build();
                builder.addMethod(propertyResolver.setter(
                    structure, name, type, true, true
                ).addCode(implementation).build());
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.setter(
                        structure, name, TypeName.get(expansionType), true, true
                    ).addCode(implementation).build()), ignored -> builder.addMethod(propertyResolver.setter(
                        structure, name, nameResolver.structure(expansion.getDescription()), true, true
                    ).addCode(implementation).build()), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
                builder.addMethod(propertyResolver.trial(structure, name, type, true, false));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.trial(
                        structure, name, TypeName.get(expansionType), true, false
                    )), ignored -> builder.addMethod(propertyResolver.trial(
                        structure, name, nameResolver.structure(expansion.getDescription()), true, false
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                builder.addMethod(propertyResolver.fluent(structure, name, type, cardinality, true, false));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.fluent(
                        structure, name, TypeName.get(expansionType), cardinality, true, false
                    )), ignored -> builder.addMethod(propertyResolver.fluent(
                        structure, name, nameResolver.structure(expansion.getDescription()), cardinality, true, false
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.MERGE)) {
                builder.addMethod(propertyResolver.merge(structure, name, type, true, null));
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.FACTORY)) {
                builder.addMethod(propertyResolver.factory(structure, name, type, null, false, () -> {
                    throw new IllegalStateException("Unexpected materialization of " + name);
                }));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.factory(
                        structure, name, type, TypeName.get(expansionType), false, () -> {
                            throw new IllegalStateException("Unexpected materialization of " + name);
                        }
                    )), ignored -> builder.addMethod(propertyResolver.factory(
                        structure, name, type, nameResolver.structure(expansion.getDescription()), false, () -> {
                            throw new IllegalStateException("Unexpected materialization of " + name);
                        }
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, null, true));
        }
    }

    private void addTypedLeaf(
        TypeSpec.Builder builder, ClassName structure, SingularDescription.Property property,
        String name, Cardinality cardinality, Class<?> owner, Class<?> type
    ) {
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            CodeBlock.Builder getter;
            switch (cardinality) {
            case SINGLE: {
                CodeBlock value = accessResolver.getter(
                    owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("this.$N", DELEGATE).build()
                );
                getter = CodeBlock.builder().addStatement(
                    "return $L",
                    typeResolver.convert(property.getDescription().getType(), type, value).orElse(value)
                );
                break;
            }
            case OPTIONAL: {
                CodeBlock value = accessResolver.getter(
                    owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("this.$N", DELEGATE).build()
                );
                getter = CodeBlock.builder().addStatement(
                    property.getDescription().getType().isPrimitive() ? "return $T.of($L)" : "return $T.ofNullable($L)",
                    Optional.class,
                    typeResolver.convert(property.getDescription().getType(), type, value).orElse(value)
                );
                break;
            }
            case LIST:
                if (property.getCardinality() == Cardinality.LIST) {
                    getter = CodeBlock.builder().add(
                        "$T.of($L, $L, $L)",
                        ProjectingList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        typeResolver.convert(
                            property.getDescription().getType(), type, CodeBlock.builder().add("value").build()
                        ).map(code ->
                            CodeBlock.builder().add("value -> $L", code)
                        ).orElseGet(() ->
                            CodeBlock.builder().add("$T.identity()", Function.class)
                        ).build(),
                        typeResolver.convert(
                            type, property.getDescription().getType(), CodeBlock.builder().add("value").build()
                        ).map(code ->
                            CodeBlock.builder().add("value -> $L", code)
                        ).orElseGet(() ->
                            CodeBlock.builder().add("$T.identity()", Function.class)
                        ).build()
                    );
                } else {
                    getter = CodeBlock.builder().add(
                        "new $T<>(() -> $L, value -> $L, $L, $L)",
                        ProjectingSingletonList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add("value").build()
                        ),
                        typeResolver.convert(
                            property.getDescription().getType(), type, CodeBlock.builder().add("value").build()
                        ).map(code ->
                            CodeBlock.builder().add("value -> $L", code)
                        ).orElseGet(() ->
                            CodeBlock.builder().add("$T.identity()", Function.class)
                        ).build(),
                        typeResolver.convert(
                            type, property.getDescription().getType(), CodeBlock.builder().add("value").build()
                        ).map(code ->
                            CodeBlock.builder().add("value -> $L", code)
                        ).orElseGet(() ->
                            CodeBlock.builder().add("$T.identity()", Function.class)
                        ).build()
                    );
                }
                if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                    getter = list(owner, property).addStatement("return $L", getter.build());
                } else {
                    getter = list(owner, property).addStatement(
                        "return $T.unmodifiableList($L)", Collections.class, getter.build()
                    );
                }
                break;
            default:
                throw new IllegalStateException();
            }
            builder.addMethod(propertyResolver.getter(structure, name, TypeName.get(type), cardinality, true, true)
                .addCode(getter.build())
                .build());
        }
        if (!name.isEmpty()) {
            if (!Collections.disjoint(propertyGenerations, EnumSet.of(
                PropertyGeneration.SETTER, PropertyGeneration.TRIAL, PropertyGeneration.FLUENT
            ))) {
                builder.addMethod(propertyResolver.setter(
                    structure, name, TypeName.get(type),
                    true, propertyGenerations.contains(PropertyGeneration.SETTER)
                ).addCode(typeResolver.convert(
                    type, property.getDescription().getType(),
                    CodeBlock.builder().add("$N", name).build()
                ).map(
                    code -> property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                        "$L.add($L)",
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        code
                    ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build(),
                        code
                    )).build()
                ).orElseGet(
                    () -> property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                        "$L.add($L)",
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        CodeBlock.builder().add("$N", name).build()
                    ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build(),
                        CodeBlock.builder().add("$N", name).build()
                    )).build()
                )).build());
            }
            if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
                builder.addMethod(propertyResolver.trial(structure, name, TypeName.get(type), true, true));
            }
            if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                builder.addMethod(propertyResolver.fluent(
                    structure, name, TypeName.get(type), cardinality, true, true
                ));
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, property.getCardinality(), true));
        }
    }

    private void addEnumeratedLeaf(
        TypeSpec.Builder builder, ClassName structure, SingularDescription.Property property,
        String name, Cardinality cardinality, Class<?> owner, TypeName type
    ) {
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            CodeBlock.Builder getter;
            switch (cardinality) {
            case SINGLE:
                getter = CodeBlock.builder().addStatement(
                    "return $T.wrap($L)",
                    type,
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    )
                );
                break;
            case OPTIONAL:
                getter = CodeBlock.builder().addStatement(
                    "return $T.ofNullable($T.wrap($L))",
                    Optional.class,
                    type,
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    )
                );
                break;
            case LIST:
                if (property.getCardinality() == Cardinality.LIST) {
                    getter = CodeBlock.builder().add(
                        "$T.of($L, $T::wrap, value -> $T.unwrap(value, $T.class))",
                        ProjectingList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        type,
                        type,
                        property.getDescription().getType()
                    );
                } else {
                    getter = CodeBlock.builder().add(
                        "new $T<>(() -> $L, value -> $L, $T::wrap, value -> $T.unwrap(value, $T.class))",
                        ProjectingSingletonList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add("value").build()
                        ),
                        type,
                        type,
                        property.getDescription().getType()
                    );
                }
                if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                    getter = list(owner, property).addStatement("return $L", getter.build());
                } else {
                    getter = list(owner, property).addStatement(
                        "return $T.unmodifiableList($L)", Collections.class, getter.build()
                    );
                }
                break;
            default:
                throw new IllegalStateException();
            }
            builder.addMethod(propertyResolver.getter(structure, name, type, cardinality, true, true)
                .addCode(getter.build())
                .build());
        }
        if (!Collections.disjoint(propertyGenerations, EnumSet.of(
            PropertyGeneration.SETTER, PropertyGeneration.TRIAL, PropertyGeneration.FLUENT
        ))) {
            builder.addMethod(propertyResolver.setter(
                structure, name, type,
                true, propertyGenerations.contains(PropertyGeneration.SETTER)
            ).addCode(
                property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                    "$L.add($L)",
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    ),
                    CodeBlock.builder().add(
                        "$T.unwrap($N, $T.class)",
                        type, name, property.getDescription().getType()
                    ).build()
                ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                    owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("this.$N", DELEGATE).build(),
                    CodeBlock.builder().add(
                        "$T.unwrap($N, $T.class)",
                        type, name, property.getDescription().getType()
                    ).build()
                )).build()
            ).build());
        }
        if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
            builder.addMethod(propertyResolver.trial(structure, name, type, true, true));
        }
        if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
            builder.addMethod(propertyResolver.fluent(structure, name, type, cardinality, true, true));
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, property.getCardinality(), true));
        }
    }

    private void addBranch(
        TypeSpec.Builder builder, ClassName structure, SingularDescription.Property property,
        String name, Cardinality cardinality, Class<?> owner, TypeName type,
        ClassName delegate, CompoundDescription compound, CompoundDescription.Property expansion
    ) {
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            CodeBlock.Builder getter;
            switch (cardinality) {
            case SINGLE:
                getter = CodeBlock.builder().addStatement(
                    "return $T.wrap($L)",
                    delegate,
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    )
                );
                break;
            case OPTIONAL:
                getter = CodeBlock.builder().addStatement(
                    "return $T.ofNullable($T.wrap($L))",
                    Optional.class,
                    delegate,
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    )
                );
                break;
            case LIST:
                if (property.getCardinality() == Cardinality.LIST) {
                    getter = CodeBlock.builder().add(
                        "$T.of($L, $T::wrap, $T::unwrap)",
                        ProjectingList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        delegate,
                        delegate
                    );
                } else {
                    getter = CodeBlock.builder().add(
                        "new $T<>(() -> $L, value -> $L, $T::wrap, $T::unwrap)",
                        ProjectingSingletonList.class,
                        accessResolver.getter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build()
                        ),
                        accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add("value").build()
                        ),
                        delegate,
                        delegate
                    );
                }
                if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                    getter = list(owner, property).addStatement("return $L", getter.build());
                } else {
                    getter = list(owner, property).addStatement(
                        "return $T.unmodifiableList($L)", Collections.class, getter.build()
                    );
                }
                break;
            default:
                throw new IllegalStateException();
            }
            builder.addMethod(propertyResolver.getter(structure, name, type, cardinality, true, true)
                .addCode(getter.build())
                .build());
        }
        if (!Collections.disjoint(propertyGenerations, EnumSet.of(
            PropertyGeneration.SETTER,
            PropertyGeneration.TRIAL,
            PropertyGeneration.FLUENT,
            PropertyGeneration.MERGE,
            PropertyGeneration.FACTORY
        ))) {
            builder.addMethod(propertyResolver.setter(
                structure, name, type,
                true, propertyGenerations.contains(PropertyGeneration.SETTER)
            ).addCode(
                property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                    "$L.add($L)",
                    accessResolver.getter(
                        owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                        CodeBlock.builder().add("this.$N", DELEGATE).build()
                    ),
                    CodeBlock.builder().add("$T.unwrap($N)", delegate, name).build()
                ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                    owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("this.$N", DELEGATE).build(),
                    CodeBlock.builder().add("$T.unwrap($N)", delegate, name).build()
                )).build()
            ).build());
            if (expansion != null) {
                if (property.getDescription().isLeaf()) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.setter(
                        structure, name, TypeName.get(expansionType),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(
                        property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                            "$L.add($L)",
                            accessResolver.getter(
                                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                                CodeBlock.builder().add("this.$N", DELEGATE).build()
                            ),
                            typeResolver.convert(
                                expansionType,
                                property.getDescription().getType(),
                                CodeBlock.builder().add("$N", name).build()
                            ).orElseGet(() -> CodeBlock.builder().add("$N", name).build())
                        ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            typeResolver.convert(
                                expansionType,
                                property.getDescription().getType(),
                                CodeBlock.builder().add("$N", name).build()
                            ).orElseGet(() -> CodeBlock.builder().add("$N", name).build())
                        )).build()
                    ).build()), ignored -> builder.addMethod(propertyResolver.setter(
                        structure, name, nameResolver.structure(expansion.getDescription()),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(
                        property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                            "$L.add($L)",
                            accessResolver.getter(
                                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                                CodeBlock.builder().add("this.$N", DELEGATE).build()
                            ),
                            CodeBlock.builder().add(
                                "$T.unwrap($N, $T.class)",
                                nameResolver.structure(expansion.getDescription()),
                                name,
                                property.getDescription().getType()
                            ).build()
                        ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add(
                                "$T.unwrap($N, $T.class)",
                                nameResolver.structure(expansion.getDescription()),
                                name,
                                property.getDescription().getType()
                            ).build()
                        )).build()
                    ).build()), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                } else {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.setter(
                        structure, name, TypeName.get(expansionType),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(
                        property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                            "$L.clear()",
                            accessResolver.getter(
                                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                                CodeBlock.builder().add("this.$N", DELEGATE).build()
                            )
                        ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add("null").build()
                        )).build()
                    ).build()), ignored -> builder.addMethod(propertyResolver.setter(
                        structure, name, nameResolver.structure(expansion.getDescription()),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(
                        property.getCardinality() == Cardinality.LIST ? list(owner, property).addStatement(
                            "$L.clear()",
                            accessResolver.getter(
                                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                                CodeBlock.builder().add("this.$N", DELEGATE).build()
                            )
                        ).build() : CodeBlock.builder().addStatement(accessResolver.setter(
                            owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                            CodeBlock.builder().add("this.$N", DELEGATE).build(),
                            CodeBlock.builder().add("null").build()
                        )).build()
                    ).build()), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
            builder.addMethod(propertyResolver.trial(structure, name, type, true, true));
            if (expansion != null) {
                expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.trial(
                    structure, name, TypeName.get(expansionType),
                    true, property.getDescription().hasProperty(CompoundDescription.EXPANSION)
                )), ignored -> builder.addMethod(propertyResolver.trial(
                    structure, name, nameResolver.structure(expansion.getDescription()),
                    true, property.getDescription().hasProperty(CompoundDescription.EXPANSION)
                )), ignored -> {
                    throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                });
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
            builder.addMethod(propertyResolver.fluent(structure, name, type, cardinality, true, true));
            if (expansion != null) {
                expansion.accept((expansionCardinality, expansionType) -> builder.addMethod(propertyResolver.fluent(
                    structure, name, TypeName.get(expansionType), cardinality,
                    true, property.getDescription().hasProperty(CompoundDescription.EXPANSION)
                )), (expansionCardinality, ignored) -> builder.addMethod(propertyResolver.fluent(
                    structure, name, nameResolver.structure(expansion.getDescription()),
                    cardinality, true, property.getDescription().hasProperty(CompoundDescription.EXPANSION)
                )), (expansionCardinality, ignored) -> {
                    throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                });
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.MERGE)) {
            if (!featureGenerations.contains(FeatureGeneration.COPY)) {
                throw new IllegalStateException("Merge property requires the copy feature to be enabled");
            }
            builder.addMethod(propertyResolver.merge(
                structure, name, type, true,
                value -> CodeBlock.builder().add(
                    "$L.copyTo($T.class).orElseThrow()",
                    value,
                    property.getDescription().getType()
                ).build()
            ));
        }
        if (propertyGenerations.contains(PropertyGeneration.FACTORY)) {
            builder.addMethod(propertyResolver.factory(
                structure, name, type, null,
                !property.getDescription().isLeaf(),
                () -> accessResolver.constructor(
                    property.getDescription().getType()
                ).map(value -> CodeBlock.builder().add(
                    "$T.wrap($L)",
                    delegate,
                    value
                ).build())
            ));
            if (expansion != null) {
                expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.factory(
                    structure, name, type, TypeName.get(expansionType),
                    property.getDescription().isLeaf(),
                    () -> Optional.of(CodeBlock.builder().add(
                        "$T.wrap($L)",
                        nameResolver.projection(compound, property.getDescription()),
                        typeResolver.convert(
                            expansionType, property.getDescription().getType(),
                            CodeBlock.builder().add("$N", name).build()
                        ).orElseGet(() -> CodeBlock.builder().add("$N", name).build())
                    ).build())
                )), ignored -> builder.addMethod(propertyResolver.factory(
                    structure, name, type, nameResolver.structure(expansion.getDescription()),
                    property.getDescription().isLeaf(),
                    () -> Optional.of(CodeBlock.builder().add(
                        "$T.wrap($T.unwrap($N, $T.class))",
                        nameResolver.projection(compound, property.getDescription()),
                        nameResolver.structure(expansion.getDescription()),
                        name,
                        property.getDescription().getType()
                    ).build())
                )), ignored -> {
                    throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                });
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, property.getCardinality(), true));
        }
    }

    private void addTypedLeafExpansion(TypeSpec.Builder builder, ClassName structure, Class<?> type, Class<?> target) {
        builder.addAnnotation(AnnotationSpec.builder(ExpansionOf.class)
            .addMember("value", CodeBlock.builder().add("$T.class", structure).build())
            .build());
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            builder.addMethod(propertyResolver.getter(
                structure, CompoundDescription.EXPANSION, TypeName.get(type), Cardinality.OPTIONAL, true, true
            ).addCode(
                typeResolver.convert(target, type, CodeBlock.builder().add(
                    "this.$N", DELEGATE
                ).build()).map(code -> CodeBlock.builder().addStatement(
                    "return $T.of($L)", Optional.class, code
                )).orElseGet(() -> CodeBlock.builder().addStatement(
                    "return $T.of(this.$N)", Optional.class, DELEGATE
                )).build()
            ).build());
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(
                structure, CompoundDescription.EXPANSION, Cardinality.SINGLE, true
            ));
        }
    }

    private void addEnumeratedLeafExpansion(TypeSpec.Builder builder, ClassName structure, TypeName type) {
        builder.addAnnotation(AnnotationSpec.builder(ExpansionOf.class)
            .addMember("value", CodeBlock.builder().add("$T.class", structure).build())
            .build());
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            builder.addMethod(propertyResolver.getter(
                structure, CompoundDescription.EXPANSION, type, Cardinality.OPTIONAL, true, true
            ).addCode(CodeBlock.builder().addStatement(
                "return $T.of($T.wrap(this.$N))",
                Optional.class,
                type,
                DELEGATE
            ).build()).build());
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(
                structure, CompoundDescription.EXPANSION, Cardinality.SINGLE, true
            ));
        }
    }

    private void addHashCode(
        TypeSpec.Builder builder,
        ClassName projection, ClassName base,
        SingularDescription singular, Map<String, CompoundDescription.Property> properties
    ) {
        if (base == null) {
            builder.addMethod(MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder().addStatement(
                    "return this.hashCode($T.newSetFromMap(new $T<>()))",
                    Collections.class, IdentityHashMap.class
                ).build())
                .build());
        }
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder().addStatement(
                "return $T.hashCode(this.$N, checked)",
                projection, DELEGATE
            ).build())
            .build());
        CodeBlock.Builder doHashCode = CodeBlock.builder().addStatement("int hashCode = 0");
        if (singular.isLeaf()) {
            if (singular.getType().isPrimitive()) {
                doHashCode.addStatement("hashCode = hashCode + $T.hashCode(value)", singular.getBoxedType());
            } else {
                doHashCode.addStatement("hashCode = hashCode + value.hashCode()");
            }
        } else {
            singular.getProperties().forEach((name, property) -> {
                Class<?> target = property.getDescription().getType();
                CodeBlock value = accessResolver.getter(
                    singular.getType(), target, property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("value").build()
                );
                if (property.getDescription().isLeaf()) {
                    if (target.isPrimitive()) {
                        doHashCode.addStatement("hashCode = hashCode + $T.hashCode($L)", TypeName.get(target).box(), value);
                    } else if (property.getCardinality() != Cardinality.LIST) {
                        doHashCode.beginControlFlow("if ($L != null)", value)
                            .addStatement("hashCode = hashCode + $L.hashCode()", value)
                            .endControlFlow();
                    } else {
                        doHashCode.addStatement("hashCode = hashCode + $L.hashCode()", value);
                    }
                } else {
                    if (property.getCardinality() == Cardinality.LIST) {
                        doHashCode.beginControlFlow("for (int index = 0; index < $L.size(); index++)", value)
                            .addStatement(
                                "hashCode = hashCode + (index + 1) * $T.hashCode($L.get(index), checked)",
                                nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                                value
                            )
                            .endControlFlow();
                    } else {
                        doHashCode.beginControlFlow("if ($L != null)", value)
                            .addStatement(
                                "hashCode = hashCode + $T.hashCode($L, checked)",
                                nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                                value
                            )
                            .endControlFlow();
                    }
                }
                doHashCode.addStatement("hashCode = hashCode * 31");
            });
        }
        builder.addMethod(MethodSpec.methodBuilder("doHashCode")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(int.class)
            .addParameter(singular.getType(), "value")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(doHashCode.addStatement("return hashCode").build())
            .build());
        CodeBlock.Builder hashCode = CodeBlock.builder();
        if (!singular.isLeaf()) {
            hashCode.beginControlFlow("if (!checked.add(value))")
                .addStatement("return 0")
                .endControlFlow();
        }
        hashCode.addStatement("$T hashCode = $T.class.hashCode()", int.class, projection);
        if (base != null) {
            hashCode.addStatement("hashCode = hashCode + 31 * $T.doHashCode(value, checked)", base);
        }
        hashCode.addStatement("hashCode = hashCode + doHashCode(value, checked)");
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(int.class)
            .addParameter(singular.getType(), "value")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(hashCode.addStatement("return hashCode").build())
            .build());
    }

    private void addEquals(
        TypeSpec.Builder builder,
        ClassName projection, ClassName base,
        SingularDescription singular, Map<String, CompoundDescription.Property> properties
    ) {
        if (base == null) {
            builder.addMethod(MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "other")
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder().addStatement(
                    "return this.equals(other, new $T<>())",
                    IdentityHashMap.class
                ).build())
                .build());
        }
        builder.addMethod(MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addParameter(Object.class, "other")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder()
                .beginControlFlow("if (other == null || this.getClass() != other.getClass())")
                .addStatement("return false")
                .endControlFlow()
                .addStatement(
                    "return $T.equals(this.$N, (($T) other).$N, checked)",
                    projection, DELEGATE, projection, DELEGATE
                )
                .build())
            .build());
        CodeBlock.Builder doEquals = CodeBlock.builder();
        if (singular.isLeaf()) {
            if (singular.getType() == float.class) {
                doEquals.addStatement("return $T.compareTo(left, right) == 0", Float.class);
            } else if (singular.getType() == double.class) {
                doEquals.addStatement("return $T.compareTo(left, right) == 0", Double.class);
            } else if (singular.getType().isPrimitive() || singular.getType().isEnum()) {
                doEquals.addStatement("return left == right");
            } else {
                doEquals.addStatement("return $T.equals(left, right)", Objects.class);
            }
        } else {
            singular.getProperties().forEach((name, property) -> {
                Class<?> target = property.getDescription().getType();
                CodeBlock left = accessResolver.getter(
                    singular.getType(), target, property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("left").build()
                ), right = accessResolver.getter(
                    singular.getType(), target, property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("right").build()
                );
                if (property.getDescription().isLeaf()) {
                    if (target == float.class) {
                        doEquals.beginControlFlow("if ($T.compareTo($L, $L) != 0)", Float.class, left, right);
                    } else if (target == double.class) {
                        doEquals.beginControlFlow("if ($T.compareTo($L, $L) != 0)", Double.class, left, right);
                    } else if (target.isPrimitive() || target.isEnum() && property.getCardinality() != Cardinality.LIST) {
                        doEquals.beginControlFlow("if ($L != $L)", left, right);
                    } else {
                        doEquals.beginControlFlow("if (!$T.equals($L, $L))", Objects.class, left, right);
                    }
                } else {
                    if (property.getCardinality() == Cardinality.LIST) {
                        doEquals.beginControlFlow(
                            "if ($L.size() != $L.size() || $T.range(0, $L.size()).anyMatch("
                                + "index -> !$T.equals($L.get(index), $L.get(index), checked)))",
                            left, right, IntStream.class, left,
                            nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                            left, right
                        );
                    } else {
                        doEquals.beginControlFlow(
                            "if (!$T.equals($L, $L, checked))",
                            nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                            left, right
                        );
                    }
                }
                doEquals.addStatement("return false").endControlFlow();
            });
            doEquals.addStatement("return true");
        }
        builder.addMethod(MethodSpec.methodBuilder("doEquals")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(boolean.class)
            .addParameter(singular.getType(), "left")
            .addParameter(singular.getType(), "right")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addCode(doEquals.build())
            .build());
        CodeBlock.Builder equals = CodeBlock.builder();
        if (!singular.isLeaf()) {
            equals.beginControlFlow("if (left == null)")
                .addStatement("return right == null")
                .nextControlFlow("else if (right == null)")
                .addStatement("return false")
                .nextControlFlow("else if (checked.containsKey(left) && checked.get(left).contains(right))")
                .addStatement("return true")
                .endControlFlow()
                .addStatement("$T current = checked.get(left)", ParameterizedTypeName.get(Set.class, Object.class))
                .beginControlFlow("if (current == null)")
                .addStatement("current = $T.newSetFromMap(new $T<>())", Collections.class, IdentityHashMap.class)
                .addStatement("checked.put(left, current)")
                .endControlFlow()
                .addStatement("current.add(right)");
        }
        if (base == null) {
            equals.addStatement("return doEquals(left, right, checked)");
        } else {
            equals.addStatement("return $T.doEquals(left, right, checked) && doEquals(left, right, checked)", base);
        }
        builder.addMethod(MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(boolean.class)
            .addParameter(singular.getType(), "left")
            .addParameter(singular.getType(), "right")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addCode(equals.build())
            .build());
    }

    private void addToString(
        TypeSpec.Builder builder,
        ClassName structure, ClassName projection, ClassName base,
        SingularDescription singular, Map<String, CompoundDescription.Property> properties
    ) {
        if (base == null) {
            builder.addMethod(MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder().addStatement(
                    "$T builder = new $T()",
                    StringBuilder.class, StringBuilder.class
                ).addStatement(
                    "this.toString(builder, $T.newSetFromMap(new $T<>()))",
                    Collections.class, IdentityHashMap.class
                ).addStatement(
                    "return builder.toString()"
                ).build())
                .build());
        }
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder().addStatement(
                "$T.toString(this.$N, builder, checked)",
                projection, DELEGATE
            ).build())
            .build());
        CodeBlock.Builder doToString = CodeBlock.builder();
        if (singular.isLeaf()) {
            doToString.addStatement("builder.append($S + $T.class.getTypeName() + $S)", " - ", projection, ": ");
            doToString.addStatement("builder.append(value)");
        } else {
            singular.getProperties().forEach((name, property) -> {
                Class<?> target = property.getDescription().getType();
                CodeBlock value = accessResolver.getter(
                    singular.getType(), target, property.getName(), property.getCardinality(),
                    CodeBlock.builder().add("value").build()
                );
                doToString.addStatement("builder.append($S)", " - " + name + ": ");
                if (property.getDescription().isLeaf()) {
                    doToString.addStatement("builder.append($L)", value);
                } else {
                    if (property.getCardinality() == Cardinality.LIST) {
                        doToString.addStatement("builder.append($S)", "[");
                        doToString.beginControlFlow("for (int index = 0; index < $L.size(); index++)", value)
                            .addStatement("builder.append(index).append($S)", ": ")
                            .addStatement(
                                "$T.toString($L.get(index), builder, checked)",
                                nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                                value
                            )
                            .endControlFlow();
                        doToString.addStatement("builder.append($S)", "]");
                    } else {
                        doToString.addStatement(
                            "$T.toString($L, builder, checked)",
                            nameResolver.projection(properties.get(name).getDescription(), property.getDescription()),
                            value
                        );
                    }
                }
            });
        }
        builder.addMethod(MethodSpec.methodBuilder("doToString")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(void.class)
            .addParameter(singular.getType(), "value")
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(doToString.build())
            .build());
        CodeBlock.Builder toString = CodeBlock.builder();
        if (singular.isLeaf()) {
            toString.addStatement(
                "builder.append($S).append($T.class.getTypeName()).append($S).append($T.class.getTypeName())",
                "Expansion of ", singular.getType(), " to structure ", structure
            );
        } else {
            toString.beginControlFlow("if (value == null)")
                .addStatement("builder.append($S)", "null")
                .addStatement("return")
                .nextControlFlow("if (!checked.add(value))")
                .addStatement(
                    "builder.append($S).append($T.identityHashCode(value))",
                    "Recursive reference to projection of ", System.class
                )
                .addStatement("return")
                .endControlFlow()
                .addStatement(
                    "builder.append($S).append($T.class.getTypeName())"
                        + ".append($S).append($T.class.getTypeName())"
                        + ".append($S).append($T.class.getTypeName())"
                        + ".append($S).append($T.identityHashCode(value))",
                    "Projection ", projection, " of ", singular.getType(), " to structure ", structure,
                    " with identity ", System.class
                );
        }
        if (base != null) {
            toString.addStatement("$T.doToString(value, builder, checked)", base);
        }
        toString.addStatement("doToString(value, builder, checked)");
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(void.class)
            .addParameter(singular.getType(), "value")
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(toString.build())
            .build());
    }

    private CodeBlock.Builder list(Class<?> owner, SingularDescription.Property property) {
        return property.getCardinality() == Cardinality.LIST ? accessResolver.list(
            owner, property.getDescription().getType(), property.getName()
        ).map(
            code -> CodeBlock.builder().beginControlFlow("if ($L == null)", accessResolver.getter(
                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                CodeBlock.builder().add("this.$N", DELEGATE).build()
            )).addStatement(accessResolver.setter(
                owner, property.getDescription().getType(), property.getName(), property.getCardinality(),
                CodeBlock.builder().add("this.$N", DELEGATE).build(),
                code
            )).endControlFlow()
        ).orElseGet(CodeBlock::builder) : CodeBlock.builder();
    }
}
