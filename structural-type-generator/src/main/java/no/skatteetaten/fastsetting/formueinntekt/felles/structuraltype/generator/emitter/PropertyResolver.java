package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.ProjectionTypeException;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.PropertyDefinition;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.Trial;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.Cardinality;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.PropertyGeneration;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;

public interface PropertyResolver {

    String MASKED = "value";

    String accessor(ClassName structure, String name, PropertyGeneration sort);

    default MethodSpec.Builder getter(
        ClassName structure, String name, TypeName type,
        Cardinality cardinality, boolean implemented, boolean declared
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.GETTER))
            .returns(cardinality.asReturnType(type));
        if (implemented) {
            return declared
                ? builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                : builder.addModifiers(Modifier.PRIVATE);
        } else {
            return builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }
    }

    default MethodSpec assume(
        ClassName structure, String name, TypeName type
    ) {
        return MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.ASSUME))
            .returns(type)
            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
            .addCode(CodeBlock.builder().addStatement(
                "return this.$N().orElseThrow(() -> new $T($S + this))",
                accessor(structure, name, PropertyGeneration.GETTER),
                NoSuchElementException.class,
                name.isEmpty() ? "Expansion property is not defined by " : ("Property '" + name + "' is not defined by ")
            ).build()).build();
    }

    default MethodSpec.Builder setter(
        ClassName structure, String name, TypeName type,
        boolean implemented, boolean declared
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.SETTER))
            .returns(void.class)
            .addParameter(type, name.isEmpty() ? MASKED : name);
        if (implemented) {
            return declared
                ? builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
                : builder.addModifiers(Modifier.PRIVATE);
        } else {
            return builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }
    }

    default MethodSpec trial(
        ClassName structure, String name, TypeName type,
        boolean implemented, boolean defined
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.TRIAL));
        if (implemented) {
            builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);
            CodeBlock.Builder code = CodeBlock.builder();
            if (defined) {
                code.beginControlFlow("try")
                    .addStatement("this.$N($N)", accessor(structure, name, PropertyGeneration.SETTER), name)
                    .addStatement("return $T.of(this)", Trial.class)
                    .nextControlFlow("catch ($T $N)", RuntimeException.class, name.equals("exception") ? "exception0" : "exception")
                    .addStatement("return $T.of(this, $N)", Trial.class, name.equals("exception") ? "exception0" : "exception")
                    .endControlFlow()
                    .build();
            } else {
                code.addStatement("return $T.of(this)", Trial.class);
            }
            builder.addCode(code.build());
        } else {
            builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }
        return builder.returns(ParameterizedTypeName.get(ClassName.get(Trial.class), WildcardTypeName.subtypeOf(structure)))
            .addParameter(type, name)
            .build();
    }

    default MethodSpec fluent(
        ClassName structure, String name, TypeName type,
        Cardinality cardinality, boolean implemented, boolean defined
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.FLUENT)).returns(structure);
        if (cardinality == Cardinality.LIST) {
            builder.addParameter(ArrayTypeName.of(type), name).varargs();
        } else {
            builder.addParameter(type, name);
        }
        if (implemented) {
            builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);
            CodeBlock.Builder code = CodeBlock.builder();
            if (defined) {
                if (cardinality == Cardinality.LIST) {
                    code.addStatement("$T.stream($N).forEach(this::$N)", Arrays.class, name, accessor(structure, name, PropertyGeneration.SETTER));
                } else {
                    code.addStatement("this.$N($N)", accessor(structure, name, PropertyGeneration.SETTER), name);
                }
            }
            builder.addCode(code.addStatement("return this").build());
        } else {
            builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }
        return builder.build();
    }

    default MethodSpec merge(
        ClassName structure, String name, TypeName type,
        boolean implemented, Function<CodeBlock, CodeBlock> copy
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.MERGE));
        if (implemented) {
            builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);
            if (copy != null) {
                builder.addCode(CodeBlock.builder()
                    .beginControlFlow("if ($N == null)", name)
                    .addStatement("this.$N(($T) null)", accessor(structure, name, PropertyGeneration.SETTER), type)
                    .nextControlFlow("else")
                    .addStatement(
                        "this.$N($L)",
                        accessor(structure, name, PropertyGeneration.SETTER),
                        copy.apply(CodeBlock.builder().add(name).build())
                    )
                    .endControlFlow()
                    .build());
            }
        } else {
            builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        }
        return builder.returns(void.class).addParameter(type, name).build();
    }

    default MethodSpec clear(
        ClassName structure, String name, TypeName type,
        Cardinality cardinality
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.CLEAR));
        builder.addModifiers(Modifier.PUBLIC, Modifier.DEFAULT);
        if (cardinality == Cardinality.LIST) {
            builder.addCode(CodeBlock.builder()
                .addStatement("this.$N().clear()", accessor(structure, name, PropertyGeneration.GETTER))
                .build());
        } else if (type == TypeName.BOOLEAN) {
            builder.addCode(CodeBlock.builder()
                .addStatement("this.$N(false)", accessor(structure, name, PropertyGeneration.SETTER))
                .build());
        } else if (type == TypeName.INT || type == TypeName.LONG || type == TypeName.FLOAT || type == TypeName.DOUBLE) {
            builder.addCode(CodeBlock.builder()
                .addStatement("this.$N(0)", accessor(structure, name, PropertyGeneration.SETTER))
                .build());
        } else if (type.isPrimitive()) {
            builder.addCode(CodeBlock.builder()
                    .addStatement("this.$N(($T) 0)", accessor(structure, name, PropertyGeneration.SETTER), type)
                    .build());
        } else {
            builder.addCode(CodeBlock.builder()
                .addStatement("this.$N(($T) null)", accessor(structure, name, PropertyGeneration.SETTER), type)
                .build());
        }
        return builder.returns(void.class).build();
    }

    default MethodSpec factory(
        ClassName structure, String name, TypeName type,
        TypeName expansion, boolean defined, Supplier<Optional<CodeBlock>> value
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.FACTORY)).returns(
            ParameterizedTypeName.get(ClassName.get(Optional.class), type)
        );
        if (expansion != null) {
            builder.addParameter(expansion, name);
        }
        if (value == null) {
            return builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build();
        } else {
            builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);
            if (defined) {
                return value.get().map(instance -> {
                    String variable;
                    if (expansion == null) {
                        variable = name;
                    } else if (name.equals("expansion")) {
                        variable = "value";
                    } else {
                        variable = "expansion";
                    }
                    return builder.addCode(CodeBlock.builder()
                        .addStatement("$T $N = $L", type, variable, instance)
                        .addStatement("this.$N($N)", accessor(structure, name, PropertyGeneration.SETTER), variable).addStatement(
                        "return $T.of($N)",
                        Optional.class, variable
                    ).build()).build();
                }).orElseGet(() -> builder.addCode(CodeBlock.builder().addStatement(
                    "throw new $T($S + $T.class.getTypeName())",
                    UnsupportedOperationException.class,
                    "Cannot instantiate projection instance for ",
                    type
                ).build()).build());
            } else {
                return builder.addCode(CodeBlock.builder().addStatement(
                    "return $T.empty()",
                    Optional.class
                ).build()).build();
            }
        }
    }

    default MethodSpec owner(
        ClassName structure, String name,
        Cardinality cardinality, boolean implemented
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(accessor(structure, name, PropertyGeneration.OWNER)).returns(
            PropertyDefinition.class
        );
        if (implemented) {
            PropertyDefinition definition;
            if (cardinality == null) {
                definition = PropertyDefinition.MISSING;
            } else {
                switch (cardinality) {
                case SINGLE:
                    definition = PropertyDefinition.SINGLE;
                    break;
                case OPTIONAL:
                    definition = PropertyDefinition.OPTIONAL;
                    break;
                case LIST:
                    definition = PropertyDefinition.LIST;
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
            return builder.addModifiers(Modifier.PUBLIC).addAnnotation(Override.class).addCode(CodeBlock.builder()
                .addStatement("return $T.$N", PropertyDefinition.class, definition.name())
                .build()).build();
        } else {
            return builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).build();
        }
    }

    default MethodSpec copyOfExpansion(
        ClassName structure
    ) {
        return MethodSpec.methodBuilder("copyOf")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(structure)
            .addParameter(structure, "original")
            .addCode(CodeBlock.builder()
                .addStatement("return copyOf(original, new $T<>())", IdentityHashMap.class)
                .build())
            .build();
    }

    default MethodSpec copyOfExpansion(
        ClassName structure, ClassName expansion
    ) {
        return MethodSpec.methodBuilder("copyOf")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(structure)
            .addParameter(structure, "original")
            .addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "copies")
            .addCode(CodeBlock.builder()
                .beginControlFlow("if (copies.containsKey(original))")
                .addStatement("return ($T) copies.get(original)", structure)
                .endControlFlow()
                .addStatement(
                    "$T copy = original.$N().map($T::of).orElse(null)",
                    structure,
                    accessor(structure, CompoundDescription.EXPANSION, PropertyGeneration.GETTER),
                    expansion
                )
                .addStatement("copies.put(original, copy)")
                .addStatement("return copy").build())
            .build();
    }

    default MethodSpec copyOf(
        ClassName structure, ClassName superType, CodeBlock value
    ) {
        CodeBlock.Builder code;
        if (value == null) {
            code = CodeBlock.builder().addStatement(
                "throw new $T($S + $T.class.getTypeName())",
                UnsupportedOperationException.class,
                "Cannot instantiate copy of ",
                structure
            );
        } else {
            code = CodeBlock.builder()
                .beginControlFlow("if (copies.containsKey(original))")
                .addStatement("return ($T) copies.get(original)", structure)
                .endControlFlow()
                .addStatement("$T copy = $L", structure, value)
                .addStatement("copies.put(original, copy)");
            if (superType != null) {
                code.addStatement("$T.copyOf(original, copy, copies)", superType);
            }
            code.addStatement("copyOf(original, copy, copies)").addStatement("return copy");
        }
        return MethodSpec.methodBuilder("copyOf")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(structure)
            .addParameter(structure, "original")
            .addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "copies")
            .addCode(code.build())
            .build();
    }

    default MethodSpec copyOf(
        ClassName structure,
        List<String> names,
        Predicate<String> terminals,
        Function<String, Cardinality> cardinalities,
        Function<String, TypeName> types,
        BiFunction<String, String, CodeBlock> copy
    ) {
        CodeBlock.Builder code = CodeBlock.builder();
        names.stream().filter(entry -> !entry.isEmpty()).forEach(name -> {
            if (terminals.test(name)) {
                switch (cardinalities.apply(name)) {
                    case SINGLE:
                        code.addStatement(
                            "copy.$N(original.$N())",
                            accessor(structure, name, PropertyGeneration.SETTER),
                            accessor(structure, name, PropertyGeneration.GETTER)
                        );
                        break;
                    case OPTIONAL:
                        code.addStatement(
                            "original.$N().ifPresent(copy::$N)",
                            accessor(structure, name, PropertyGeneration.GETTER),
                            accessor(structure, name, PropertyGeneration.SETTER)
                        );
                        break;
                    case LIST:
                        code.addStatement(
                            "copy.$N().addAll(original.$N())",
                            accessor(structure, name, PropertyGeneration.GETTER),
                            accessor(structure, name, PropertyGeneration.GETTER)
                        );
                        break;
                    default:
                        throw new IllegalStateException();
                }
            } else {
                String variable = name;
                if (variable.equals("copy") || variable.equals("original") || variable.equals("copies")) {
                    int index = 0;
                    do {
                        variable = name + index++;
                    } while (names.contains(variable));
                }
                switch (cardinalities.apply(name)) {
                    case SINGLE:
                        code.addStatement(
                            "$T $N = original.$N()",
                            types.apply(name),
                            variable,
                            accessor(structure, name, PropertyGeneration.GETTER)
                        ).beginControlFlow(
                            "if ($N != null)",
                            variable
                        ).addStatement(
                            "copy.$N($N.$L)",
                            accessor(structure, name, PropertyGeneration.SETTER),
                            variable,
                            copy.apply(name, "copies")
                        ).endControlFlow();
                        break;
                    case OPTIONAL:
                        code.addStatement(
                            "original.$N().map($N -> $N.$L).ifPresent(copy::$N)",
                            accessor(structure, name, PropertyGeneration.GETTER),
                            variable,
                            variable,
                            copy.apply(name, "copies"),
                            accessor(structure, name, PropertyGeneration.SETTER)
                        );
                        break;
                    case LIST:
                        code.addStatement(
                            "original.$N().stream().map($N -> $N.$L).forEach(copy.$N()::add)",
                            accessor(structure, name, PropertyGeneration.GETTER),
                            variable,
                            variable,
                            copy.apply(name, "copies"),
                            accessor(structure, name, PropertyGeneration.GETTER)
                        );
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        });
        return MethodSpec.methodBuilder("copyOf")
            .addModifiers(Modifier.PROTECTED, Modifier.STATIC)
            .returns(void.class)
            .addParameter(structure, "original")
            .addParameter(structure, "copy")
            .addParameter(ParameterizedTypeName.get(Map.class, Object.class, Object.class), "copies")
            .addCode(code.build())
            .build();
    }
}
