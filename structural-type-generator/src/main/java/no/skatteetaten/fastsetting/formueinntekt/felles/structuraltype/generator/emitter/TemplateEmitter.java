package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import com.squareup.javapoet.*;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.TemplateOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.FeatureGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.PropertyGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

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
        compound.getSuperDescription().ifPresent(superCompound -> builder.superclass(nameResolver.template(superCompound)));
        if (featureGenerations.contains(FeatureGeneration.COPY)) {
            if (!propertyGenerations.containsAll(EnumSet.of(PropertyGeneration.GETTER, PropertyGeneration.SETTER))) {
                throw new IllegalStateException("The copy feature requires getter and setter properties to be enabled");
            }
            builder.addMethod(propertyResolver.copyOf(
                structure,
                compound.getSuperDescription().map(nameResolver::template).orElse(null),
                CodeBlock.builder().add("new $T()", template).build()
            )).addMethod(propertyResolver.copyOf(
                structure,
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
        if (featureGenerations.contains(FeatureGeneration.READ_DELEGATE) && compound.getSuperDescription().isEmpty()) {
            builder.addMethod(MethodSpec.methodBuilder("delegate")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), WildcardTypeName.subtypeOf(Object.class)))
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
            addHashCode(builder, template, compound.getSuperDescription()
                .map(nameResolver::template)
                .orElse(null), properties);
            addEquals(builder, template, compound.getSuperDescription()
                .map(nameResolver::template)
                .orElse(null), properties);
        }
        if (featureGenerations.contains(FeatureGeneration.TO_STRING)) {
            addToString(builder, structure, template, compound.getSuperDescription()
                .map(nameResolver::template)
                .orElse(null), properties);
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
                .addCode(CodeBlock.builder().addStatement("this.$N = $N", variable, variable).build())
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
                getter = CodeBlock.builder().addStatement("return $T.ofNullable(this.$N)", Optional.class, variable);
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
        ClassName base,
        Map<String, CompoundDescription.Property> properties
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
        CodeBlock.Builder doHashCode = CodeBlock.builder().addStatement("int hashCode = 0");
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            property.accept((cardinality, type) -> {
                if (type.isPrimitive()) {
                    doHashCode.addStatement(
                        "hashCode = hashCode + $T.hashCode(value.$N)",
                        TypeName.get(type).box(), variable
                    );
                } else {
                    doHashCode.beginControlFlow("if (value.$N != null)", variable)
                        .addStatement("hashCode = hashCode + value.$N.hashCode()", variable)
                        .endControlFlow();
                }
            }, (cardinality, ignored) -> doHashCode.beginControlFlow(
                "if (value.$N != null)", variable
            ).addStatement("hashCode = 31 * value.$N.hashCode()", variable).endControlFlow(), (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    doHashCode.beginControlFlow("if (value.$N != null)", variable)
                        .addStatement("hashCode = 31 * hashCode")
                        .beginControlFlow("for (int index = 0; index < value.$N.size(); index++)", variable)
                        .addStatement(
                            "hashCode = hashCode + (index + 1) * value.$N.get(index).hashCode(checked)",
                            variable
                        )
                        .endControlFlow()
                        .endControlFlow();
                } else {
                    doHashCode.beginControlFlow("if (value.$N != null)", variable)
                        .addStatement("hashCode = 31 * hashCode + value.$N.hashCode(checked)", variable)
                        .endControlFlow();
                }
            });
            doHashCode.addStatement("hashCode = 31 * hashCode");
        });
        builder.addMethod(MethodSpec.methodBuilder("doHashCode")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(int.class)
            .addParameter(template, "value")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(doHashCode.addStatement("return hashCode").build())
            .build());
        CodeBlock.Builder hashCode = CodeBlock.builder()
            .beginControlFlow("if (!checked.add(this))")
            .addStatement("return 0")
            .endControlFlow()
            .addStatement("$T hashCode = $T.class.hashCode()", int.class, template);
        if (base != null) {
            hashCode.addStatement("hashCode = hashCode + 31 * $T.doHashCode(this, checked)", base);
        }
        hashCode.addStatement("hashCode = hashCode + doHashCode(this, checked)");
        builder.addMethod(MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(hashCode.addStatement("return hashCode").build())
            .build());
    }

    private void addEquals(
        TypeSpec.Builder builder,
        ClassName template,
        ClassName base,
        Map<String, CompoundDescription.Property> properties
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
        CodeBlock.Builder doEquals = CodeBlock.builder();
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            property.accept((cardinality, type) -> {
                if (type == float.class) {
                    doEquals.beginControlFlow("if ($T.compareTo(left.$N, right.$N) != 0)", Float.class, variable, variable);
                } else if (type == double.class) {
                    doEquals.beginControlFlow("if ($T.compareTo(left.$N, right.$N) != 0)", Float.class, variable, variable);
                } else if (type.isPrimitive()) {
                    doEquals.beginControlFlow("if (left.$N != right.$N)", variable, variable);
                } else if (cardinality == Cardinality.LIST) {
                    doEquals.beginControlFlow(
                        "if (left.$N == null && right.$N != null && !right.$N.isEmpty() "
                            + "|| left.$N != null && right.$N == null && !left.$N.isEmpty() "
                            + "|| left.$N != null && right.$N != null && !left.$N.equals(right.$N))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable
                    );
                } else {
                    doEquals.beginControlFlow("if (!$T.equals(left.$N, right.$N))", Objects.class, variable, variable);
                }
            }, (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    doEquals.beginControlFlow(
                        "if (left.$N == null && right.$N != null && !right.$N.isEmpty() "
                            + "|| left.$N != null && right.$N == null && !left.$N.isEmpty() "
                            + "|| left.$N != null && right.$N != null && !left.$N.equals(right.$N))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable
                    );
                } else {
                    doEquals.beginControlFlow("if (left.$N != right.$N)", variable, variable);
                }
            }, (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    doEquals.beginControlFlow(
                        "if (left.$N == null && right.$N != null && !right.$N.isEmpty() "
                            + "|| left.$N != null && right.$N == null && !left.$N.isEmpty() "
                            + "|| left.$N != null && right.$N != null && (left.$N.size() != right.$N.size()"
                            + "|| $T.range(0, left.$N.size()).anyMatch("
                            + "index -> left.$N.get(index) == null && right.$N.get(index) != null "
                            + "|| !left.$N.get(index).equals(right.$N.get(index), checked))))",
                        variable, variable, variable,
                        variable, variable, variable,
                        variable, variable, variable, variable,
                        IntStream.class, variable,
                        variable, variable,
                        variable, variable
                    );
                } else {
                    doEquals.beginControlFlow(
                        "if (left.$N == null && right.$N != null "
                            + "|| left.$N != null && !left.$N.equals(right.$N, checked))",
                        variable, variable,
                        variable, variable, variable
                    );
                }
            });
            doEquals.addStatement("return false").endControlFlow();
        });
        builder.addMethod(MethodSpec.methodBuilder("doEquals")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(boolean.class)
            .addParameter(template, "left")
            .addParameter(template, "right")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addCode(doEquals.addStatement("return true").build())
            .build());
        CodeBlock.Builder equals = CodeBlock.builder()
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
            .addStatement("current.add(other)");
        if (base != null) {
            equals.addStatement(
                "return $T.doEquals(this, ($T) other, checked) && doEquals(this, ($T) other, checked)",
                base, template, template
            );
        } else {
            equals.addStatement("return doEquals(this, ($T) other, checked)", template);
        }
        builder.addMethod(MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .returns(boolean.class)
            .addParameter(Object.class, "other")
            .addParameter(ParameterizedTypeName.get(
                ClassName.get(Map.class), ClassName.OBJECT, ParameterizedTypeName.get(Set.class, Object.class)
            ), "checked")
            .addAnnotation(Override.class)
            .addCode(equals.build())
            .build());
    }

    private void addToString(
        TypeSpec.Builder builder,
        ClassName structure, ClassName template, ClassName base,
        Map<String, CompoundDescription.Property> properties
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
                ).addStatement("return builder.toString()").build())
                .build());
        }
        CodeBlock.Builder doToString = CodeBlock.builder();
        properties.forEach((name, property) -> {
            String variable = toVariable(name, properties.keySet());
            doToString.addStatement("builder.append($S).append($S).append($S)", " - ", variable, ": ");
            property.accept((cardinality, type) -> doToString.addStatement(
                "builder.append(value.$N)", variable
            ), (cardinality, ignored) -> doToString.addStatement(
                "builder.append(value.$N)", variable
            ), (cardinality, ignored) -> {
                if (cardinality == Cardinality.LIST) {
                    doToString.beginControlFlow("if (value.$N == null)", variable)
                        .addStatement("builder.append($S)", "null")
                        .nextControlFlow("else")
                        .addStatement("builder.append($S)", "[")
                        .beginControlFlow("for (int index = 0; index < value.$N.size(); index++)", variable)
                        .addStatement(
                            "builder.append($N).append($S).append(value.$N.get(index))",
                            "index", ": ", variable
                        )
                        .endControlFlow()
                        .addStatement("builder.append($S)", "]")
                        .endControlFlow();
                } else {
                    doToString.addStatement("builder.append(value.$N)", variable);
                }
            });
        });
        builder.addMethod(MethodSpec.methodBuilder("doToString")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(void.class)
            .addParameter(StringBuilder.class, "builder")
            .addParameter(template, "value")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addCode(doToString.build())
            .build());
        CodeBlock.Builder toString = CodeBlock.builder()
            .beginControlFlow("if (!checked.add(this))")
            .addStatement(
                "builder.append($S).append($T.identityHashCode(this))",
                "Recursive reference to template ", System.class
            )
            .addStatement("return")
            .endControlFlow()
            .addStatement(
                "builder.append($S).append($T.class.getTypeName())"
                    + ".append($S).append($T.class.getTypeName())"
                    + ".append($S).append($T.identityHashCode(this))",
                "Template ", template, " of structure ", structure,
                " with identity ", System.class
            );
        if (base != null) {
            toString.addStatement("$T.doToString(builder, this, checked)", base);
        }
        toString.addStatement("doToString(builder, this, checked)");
        builder.addMethod(MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .returns(void.class)
            .addParameter(StringBuilder.class, "builder")
            .addParameter(ParameterizedTypeName.get(Set.class, Object.class), "checked")
            .addAnnotation(Override.class)
            .addCode(toString.build())
            .build());
    }
}
