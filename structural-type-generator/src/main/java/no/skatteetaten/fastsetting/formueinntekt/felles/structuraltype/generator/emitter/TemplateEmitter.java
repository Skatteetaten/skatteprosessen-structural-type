package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplateOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.FeatureGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.PropertyGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;

public class TemplateEmitter implements BiConsumer<CompoundDescription, Map<String, CompoundDescription.Property>> {

    private final Set<PropertyGeneration> propertyGenerations;
    private final Set<FeatureGeneration> featureGenerations;
    private final NameResolver nameResolver;
    private final PropertyResolver propertyResolver;
    private final BiConsumer<ClassName, JavaFile> consumer;

    public TemplateEmitter(
        Set<PropertyGeneration> propertyGenerations,
        Set<FeatureGeneration> featureGenerations,
        NameResolver nameResolver,
        PropertyResolver propertyResolver,
        BiConsumer<ClassName, JavaFile> consumer
    ) {
        this.propertyGenerations = propertyGenerations;
        this.featureGenerations = featureGenerations;
        this.nameResolver = nameResolver;
        this.propertyResolver = propertyResolver;
        this.consumer = consumer;
    }

    @Override
    public void accept(CompoundDescription compound, Map<String, CompoundDescription.Property> properties) {
        ClassName structure = nameResolver.structure(compound);
        ClassName template = nameResolver.template(compound);
        TypeSpec.Builder builder = TypeSpec.classBuilder(template)
            .addSuperinterface(structure)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(TemplateOf.class)
                .addMember("value", CodeBlock.builder().add("$T.class", structure).build())
                .build());
        if (featureGenerations.contains(FeatureGeneration.COPY)) {
            if (!propertyGenerations.containsAll(EnumSet.of(PropertyGeneration.GETTER, PropertyGeneration.SETTER))) {
                throw new IllegalStateException("The copy feature requires getter and setter properties to be enabled");
            }
            builder.addMethod(propertyResolver.copyOf(
                structure,
                CodeBlock.builder().add("new $T()", template).build(),
                new ArrayList<>(properties.keySet()),
                name -> properties.get(name).getDescription().getSort() != CompoundDescription.Sort.BRANCH,
                name -> properties.get(name).getCardinality(),
                name -> properties.get(name).getDescription().apply(
                    TypeName::get,
                    ignored -> nameResolver.structure(properties.get(name).getDescription()),
                    ignored -> nameResolver.structure(properties.get(name).getDescription())
                ),
                (name, copies) -> CodeBlock.builder().add("copyToTemplate($N)", copies).build()
            ));
            builder.addMethod(MethodSpec.methodBuilder("copy")
                .addModifiers(Modifier.PUBLIC)
                .returns(structure)
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder().addStatement("return this.copyToTemplate()").build())
                .build());
        }
        if (featureGenerations.contains(FeatureGeneration.READ_DELEGATE)) {
            builder.addMethod(MethodSpec.methodBuilder("delegate")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Optional.class, Object.class))
                .addAnnotation(Override.class)
                .addCode(CodeBlock.builder().addStatement("return $T.empty()", Optional.class).build()).build());
        }
        properties.forEach((name, property) -> property.accept((cardinality, type) -> addProperty(
            builder, structure, property.getDescription(), false,
            name, cardinality, TypeName.get(type), null, properties.keySet()
        ), (cardinality, ignored) -> addProperty(
            builder, structure, property.getDescription(), false,
            name, cardinality, nameResolver.structure(property.getDescription()), null, properties.keySet()
        ), (cardinality, nested) -> addProperty(
            builder, structure, property.getDescription(), true,
            name, cardinality, nameResolver.structure(property.getDescription()),
            nested.get(CompoundDescription.EXPANSION), properties.keySet()
        )));
        if (featureGenerations.contains(FeatureGeneration.HASHCODE_EQUALS)) {
            addHashCode(builder, template, properties);
            addEquals(builder, template, properties);
        }
        if (featureGenerations.contains(FeatureGeneration.TO_STRING)) {
            addToString(builder, structure, template, properties);
        }
        consumer.accept(template, JavaFile.builder(
            template.packageName(), builder.build()
        ).skipJavaLangImports(true).build());
    }

    private static String toVariable(String name, Set<String> names) {
        if (name.isEmpty()) {
            String candidate = PropertyResolver.MASKED;
            int index = 0;
            while (names.contains(candidate)) {
                candidate = name + index++;
            }
            return candidate;
        } else {
            return name;
        }
    }

    private void addProperty(
        TypeSpec.Builder builder, ClassName structure, CompoundDescription target, boolean branch,
        String name, Cardinality cardinality, TypeName type, CompoundDescription.Property expansion,
        Set<String> names
    ) {
        String variable = toVariable(name, names);
        if (name.isEmpty()) {
            if (cardinality != Cardinality.OPTIONAL) {
                throw new IllegalStateException("Unexpected expansion property cardinality: " + cardinality);
            }
            builder.addField(FieldSpec.builder(cardinality.asPropertyType(type), variable)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());
            builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addCode(CodeBlock.builder().addStatement("this.$N = null", variable).build())
                .build());
            builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(cardinality.asPropertyType(type), variable)
                    .build())
                .addCode(CodeBlock.builder().addStatement("this.$N = $N",
                    variable,
                    variable).build())
                .build());
        } else {
            builder.addField(FieldSpec.builder(cardinality.asPropertyType(type), name)
                .addModifiers(Modifier.PRIVATE)
                .build());
        }
        if (propertyGenerations.contains(PropertyGeneration.GETTER)) {
            CodeBlock.Builder getter;
            switch (cardinality) {
            case SINGLE:
                getter = CodeBlock.builder().addStatement("return this.$N", variable);
                break;
            case OPTIONAL:
                getter = CodeBlock.builder().addStatement("return $T.ofNullable(this.$N)",
                    Optional.class,
                    variable);
                break;
            case LIST:
                getter = CodeBlock.builder()
                    .beginControlFlow("if (this.$N == null)", variable)
                    .addStatement("this.$N = new $T<>()", variable, ArrayList.class)
                    .endControlFlow();
                if (propertyGenerations.contains(PropertyGeneration.SETTER)) {
                    getter.addStatement("return this.$N", variable);
                } else {
                    getter.addStatement("return $T.unmodifiableList(this.$N)", Collections.class, variable);
                }
                break;
            default:
                throw new IllegalStateException();
            }
            builder.addMethod(propertyResolver.getter(structure, name, type, cardinality, true, true)
                .addCode(getter.build())
                .build());
        }
        if (!name.isEmpty()) {
            if (!Collections.disjoint(propertyGenerations, EnumSet.of(
                PropertyGeneration.SETTER, PropertyGeneration.TRIAL, PropertyGeneration.FLUENT
            )) || branch && !Collections.disjoint(propertyGenerations, EnumSet.of(
                PropertyGeneration.MERGE, PropertyGeneration.FACTORY
            ))) {
                builder.addMethod(propertyResolver.setter(
                    structure, name, type,
                    true, propertyGenerations.contains(PropertyGeneration.SETTER)
                ).addCode(cardinality == Cardinality.LIST ? CodeBlock.builder().beginControlFlow(
                    "if (this.$N == null)",
                    variable
                ).addStatement(
                    "this.$N = new $T<>()",
                    variable, ArrayList.class
                ).endControlFlow().addStatement(
                    "this.$N.add($N)", variable, variable
                ).build() : CodeBlock.builder().addStatement(
                    "this.$N = $N",
                    variable, variable
                ).build()).build());
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.setter(
                        structure, name, TypeName.get(expansionType),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(cardinality == Cardinality.LIST ? CodeBlock.builder().beginControlFlow(
                        "if (this.$N == null)",
                        variable
                    ).addStatement(
                        "this.$N = new $T<>()",
                        variable, ArrayList.class
                    ).endControlFlow().addStatement(
                        "this.$N.add(new $T($N))",
                        variable, nameResolver.template(target), variable
                    ).build() : CodeBlock.builder().addStatement(
                        "this.$N = new $T($N)",
                        variable, nameResolver.template(target), variable
                    ).build()).build()), ignored -> builder.addMethod(propertyResolver.setter(
                        structure, name, nameResolver.structure(expansion.getDescription()),
                        true, propertyGenerations.contains(PropertyGeneration.SETTER)
                    ).addCode(cardinality == Cardinality.LIST ? CodeBlock.builder().beginControlFlow(
                        "if (this.$N == null)",
                        variable
                    ).addStatement(
                        "this.$N = new $T<>()",
                        variable, ArrayList.class
                    ).endControlFlow().addStatement(
                        "this.$N.add(new $T($N))",
                        variable, nameResolver.template(target), variable
                    ).build() : CodeBlock.builder().addStatement(
                        "this.$N = new $T($N)",
                        variable, nameResolver.template(target), variable
                    ).build()).build()), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.TRIAL)) {
                builder.addMethod(propertyResolver.trial(structure, name, type, true, true));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.trial(
                        structure, name, TypeName.get(expansionType), true, true
                    )), ignored -> builder.addMethod(propertyResolver.trial(
                        structure, name, nameResolver.structure(expansion.getDescription()), true, true
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (propertyGenerations.contains(PropertyGeneration.FLUENT)) {
                builder.addMethod(propertyResolver.fluent(structure, name, type, cardinality, true, true));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.fluent(
                        structure, name, TypeName.get(expansionType),
                        cardinality, true, true
                    )), ignored -> builder.addMethod(propertyResolver.fluent(
                        structure, name, nameResolver.structure(expansion.getDescription()),
                        cardinality, true, true
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.MERGE)) {
                if (!featureGenerations.contains(FeatureGeneration.COPY)) {
                    throw new IllegalStateException("Merge property requires the copy feature to be enabled");
                }
                builder.addMethod(propertyResolver.merge(
                    structure, name, type, true,
                    value -> CodeBlock.builder().add("$L.copyToTemplate()", value).build()
                ));
            }
            if (branch && propertyGenerations.contains(PropertyGeneration.FACTORY)) {
                builder.addMethod(propertyResolver.factory(
                    structure, name, type, null,
                    target.getSort() == CompoundDescription.Sort.BRANCH,
                    () -> Optional.of(CodeBlock.builder().add("new $T()", nameResolver.template(target)).build())
                ));
                if (expansion != null) {
                    expansion.getDescription().accept(expansionType -> builder.addMethod(propertyResolver.factory(
                        structure, name, type, TypeName.get(expansionType),
                        target.getSort() == CompoundDescription.Sort.BRANCH,
                        () -> Optional.of(CodeBlock.builder().add(
                            "new $T($N)",
                            nameResolver.template(target),
                            name
                        ).build())
                    )), ignored -> builder.addMethod(propertyResolver.factory(
                        structure, name, type, nameResolver.structure(expansion.getDescription()),
                        target.getSort() == CompoundDescription.Sort.BRANCH,
                        () -> Optional.of(CodeBlock.builder().add(
                            "new $T($N)",
                            nameResolver.template(target),
                            name
                        ).build())
                    )), ignored -> {
                        throw new IllegalStateException("Unexpected branch for expansion property of " + type);
                    });
                }
            }
        }
        if (propertyGenerations.contains(PropertyGeneration.OWNER)) {
            builder.addMethod(propertyResolver.owner(structure, name, cardinality, true));
        }
    }

    private void addHashCode(
        TypeSpec.Builder builder,
        ClassName template,
        Map<String, CompoundDescription.Property> properties
    ) {
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder().addStatement(
                "return this.hashCode($T.newSetFromMap(new $T<>()))",
                Collections.class, IdentityHashMap.class
            ).build())
            .build());
        CodeBlock.Builder code = CodeBlock.builder()
            .beginControlFlow("if (!checked.add(this))")
            .addStatement("return 0")
            .endControlFlow()
            .addStatement("$T hashCode = $T.class.hashCode()", int.class, template);
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            code.addStatement("hashCode = 31 * hashCode");
            property.accept((cardinality, type) -> {
                if (type.isPrimitive()) {
                    code.addStatement(
                        "hashCode = hashCode + $T.hashCode(this.$N)",
                        TypeName.get(type).box(), variable
                    );
                } else {
                    code.beginControlFlow("if (this.$N != null)", variable)
                        .addStatement("hashCode = hashCode + this.$N.hashCode()", variable)
                        .endControlFlow();
                }
            }, (cardinality, ignored) -> code.beginControlFlow(
                "if (this.$N != null)", variable
            ).addStatement("hashCode = 31 * this.$N.hashCode()", variable).endControlFlow(), (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    code.beginControlFlow("if (this.$N != null)", variable)
                        .addStatement("hashCode = 31 * hashCode")
                        .beginControlFlow("for (int index = 0; index < this.$N.size(); index++)", variable)
                        .addStatement(
                            "hashCode = hashCode + (index + 1) * this.$N.get(index).hashCode(checked)",
                            variable
                        )
                        .endControlFlow()
                        .endControlFlow();
                } else {
                    code.beginControlFlow("if (this.$N != null)", variable)
                        .addStatement("hashCode = 31 * hashCode + this.$N.hashCode(checked)", variable)
                        .endControlFlow();
                }
            });
        });
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(code.addStatement("return hashCode").build())
            .build());
    }

    private void addEquals(
        TypeSpec.Builder builder,
        ClassName template,
        Map<String, CompoundDescription.Property> properties
    ) {
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
        CodeBlock.Builder code = CodeBlock.builder()
            .beginControlFlow("if (other == null || this.getClass() != other.getClass())")
            .addStatement("return false")
            .nextControlFlow("if (checked.containsKey(this) && checked.get(this).contains(other))")
            .addStatement("return true")
            .endControlFlow()
            .addStatement("$T current = checked.get(this)", ParameterizedTypeName.get(Set.class, Object.class))
            .beginControlFlow("if (current == null)")
            .addStatement("current = $T.newSetFromMap(new $T<>())", Collections.class, IdentityHashMap.class)
            .addStatement("checked.put(this, current)")
            .endControlFlow()
            .addStatement("current.add(other)")
            .addStatement("$T value = ($T) other", template, template);
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            property.accept((cardinality, type) -> {
                if (type == float.class) {
                    code.beginControlFlow("if ($T.compareTo(this.$N, value.$N) != 0)", Float.class, variable, variable);
                } else if (type == double.class) {
                    code.beginControlFlow("if ($T.compareTo(this.$N, value.$N) != 0)", Float.class, variable, variable);
                } else if (type.isPrimitive()) {
                    code.beginControlFlow("if (this.$N != value.$N)", variable, variable);
                } else if (cardinality == Cardinality.LIST) {
                    code.beginControlFlow(
                        "if (this.$N == null && value.$N != null && !value.$N.isEmpty() "
                            + "|| this.$N != null && value.$N == null && !this.$N.isEmpty() "
                            + "|| this.$N != null && value.$N != null && !this.$N.equals(value.$N))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable
                    );
                } else {
                    code.beginControlFlow("if (!$T.equals(this.$N, value.$N))", Objects.class, variable, variable);
                }
            }, (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    code.beginControlFlow(
                        "if (this.$N == null && value.$N != null && !value.$N.isEmpty() "
                            + "|| this.$N != null && value.$N == null && !this.$N.isEmpty() "
                            + "|| this.$N != null && value.$N != null && !this.$N.equals(value.$N))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable
                    );
                } else {
                    code.beginControlFlow("if (this.$N != value.$N)", variable, variable);
                }
            }, (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    code.beginControlFlow(
                        "if (this.$N == null && value.$N != null && !value.$N.isEmpty() "
                            + "|| this.$N != null && value.$N == null && !this.$N.isEmpty() "
                            + "|| this.$N != null && value.$N != null && (this.$N.size() != value.$N.size()"
                            + "|| $T.range(0, this.$N.size()).anyMatch("
                            + "index -> this.$N.get(index) == null && value.$N.get(index) != null "
                            + "|| !this.$N.get(index).equals(value.$N.get(index), checked))))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable,
                        IntStream.class, variable,
                        variable, variable,
                        variable, variable
                    );
                } else {
                    code.beginControlFlow(
                        "if (this.$N == null && value.$N != null "
                            + "|| this.$N != null && !this.$N.equals(value.$N, checked))",
                        variable, variable,
                        variable, variable, variable
                    );
                }
            });
            code.addStatement("return false").endControlFlow();
        });
        builder.addMethod(MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addParameter(Object.class, "other")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addAnnotation(Override.class)
            .addCode(code.addStatement("return true").build())
            .build());
    }

    private void addToString(
        TypeSpec.Builder builder,
        ClassName structure, ClassName template,
        Map<String, CompoundDescription.Property> properties
    ) {
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addAnnotation(Override.class)
            .addCode(CodeBlock.builder().addStatement(
                "$T stringBuilder = new $T()",
                StringBuilder.class, StringBuilder.class
            ).addStatement(
                "this.toString(stringBuilder, $T.newSetFromMap(new $T<>()))",
                Collections.class, IdentityHashMap.class
            ).addStatement("return stringBuilder.toString()").build())
            .build());
        CodeBlock.Builder code = CodeBlock.builder()
            .beginControlFlow("if (!checked.add(this))")
            .addStatement(
                "builder.append($S).append($T.identityHashCode(this))",
                "Recursive reference to template ", System.class
            )
            .addStatement("return")
            .endControlFlow()
            .addStatement(
                "builder.append($S).append($T.class.getTypeName()).append($S).append($T.class.getTypeName())"
                    + ".append($S).append($T.identityHashCode(this))",
                "Template ", template, " of structure ", structure,
                " with identity ", System.class
            );
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            code.addStatement("builder.append($S).append($S).append($S)", " - ", variable, ": ");
            property.accept((cardinality, type) -> code.addStatement(
                "builder.append(this.$N)", variable
            ), (cardinality, ignored) -> code.addStatement(
                "builder.append(this.$N)", variable
            ), (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    code.beginControlFlow("if (this.$N == null)", variable)
                        .addStatement("builder.append($S)", "null")
                        .nextControlFlow("else")
                        .addStatement("builder.append($S)", "[")
                        .beginControlFlow("for (int index = 0; index < this.$N.size(); index++)", variable)
                        .addStatement(
                            "builder.append($N).append($S).append(this.$N.get(index))",
                            "index", ": ", variable
                        )
                        .endControlFlow()
                        .addStatement("builder.append($S)", "]")
                        .endControlFlow();
                } else {
                    code.addStatement("builder.append(this.$N)", variable);
                }
            });
        });
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(code.build())
            .build());
    }
}
