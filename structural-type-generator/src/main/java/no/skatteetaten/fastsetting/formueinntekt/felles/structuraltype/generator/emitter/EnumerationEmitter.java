package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.EnumeratedAs;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.EnumerationOf;

public class EnumerationEmitter implements BiConsumer<CompoundDescription, Map<String, Map<Class<?>, Enum<?>>>> {

    private final NameResolver nameResolver;
    private final BiConsumer<ClassName, JavaFile> consumer;

    public EnumerationEmitter(
        NameResolver nameResolver,
        BiConsumer<ClassName, JavaFile> consumer
    ) {
        this.nameResolver = nameResolver;
        this.consumer = consumer;
    }

    @Override
    public void accept(CompoundDescription compound, Map<String, Map<Class<?>, Enum<?>>> constants) {
        ClassName structure = nameResolver.structure(compound);
        TypeSpec.Builder builder = TypeSpec.enumBuilder(structure)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(EnumerationOf.class).addMember("value", CodeBlock.builder().add(
                "{ " + String.join(", ", Collections.nCopies(compound.getSingulars().size(), "$T.class")) + " }",
                compound.getSingulars().stream().map(SingularDescription::getType).toArray(Object[]::new)
            ).build()).build());
        Map<String, String> normalized = constants.keySet().stream().collect(Collectors.toMap(
            Function.identity(),
            constant -> constant.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(),
            (left, right) -> {
                throw new IllegalStateException();
            },
            LinkedHashMap::new
        ));
        CodeBlock.Builder valueOfName = CodeBlock.builder().beginControlFlow("switch (name)");
        normalized.forEach((constant, name) -> {
            if (constant.equals(name)) {
                builder.addEnumConstant(name);
            } else {
                builder.addEnumConstant(name, TypeSpec.anonymousClassBuilder("")
                    .addAnnotation(AnnotationSpec.builder(EnumeratedAs.class)
                        .addMember("value", "$S", constant)
                        .build())
                    .build());
            }
            valueOfName.add("case $S:\n", constant)
                    .indent()
                    .addStatement("return $T.$N", structure, name)
                    .unindent();
        });
        CodeBlock.Builder unwrap = CodeBlock.builder()
            .beginControlFlow("if (value == null)")
            .addStatement("return null");
        Map<Enum<?>, String> names = constants.entrySet().stream()
            .flatMap(entry -> entry.getValue().values().stream().map(enumeration -> Map.entry(enumeration, normalized.get(entry.getKey()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        compound.getSingulars().forEach(singular -> {
            CodeBlock.Builder wrap = CodeBlock.builder()
                .beginControlFlow("if (value == null)")
                .addStatement("return null")
                .endControlFlow()
                .beginControlFlow("switch (value)");
            @SuppressWarnings("unchecked")
            Enum<?>[] enumerations = ((Class<? extends Enum<?>>) singular.getType()).getEnumConstants();
            for (Enum<?> enumeration : enumerations) {
                wrap.add("case $N:\n", enumeration.name())
                    .indent()
                    .addStatement("return $T.$N", structure, names.get(enumeration))
                    .unindent();
            }
            builder.addMethod(MethodSpec.methodBuilder("wrap")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(structure)
                .addParameter(singular.getType(), "value")
                .addCode(wrap.add("default:\n")
                    .indent()
                    .addStatement("throw new $T(value.toString())", IllegalStateException.class)
                    .unindent()
                    .endControlFlow()
                    .build())
                .build());
            unwrap.nextControlFlow("else if (type == $T.class)", singular.getType()).beginControlFlow("switch (value)");
            constants.forEach((constant, values) -> {
                unwrap.add("case $N:\n", normalized.get(constant)).indent();
                if (values.containsKey(singular.getType())) {
                    unwrap.addStatement("return (E) $T.$N", singular.getType(), values.get(singular.getType()).name());
                } else {
                    unwrap.addStatement("return null");
                }
                unwrap.unindent();
            });
            unwrap.add("default:\n")
                .indent()
                .addStatement("throw new $T(value.toString())", IllegalStateException.class)
                .unindent()
                .endControlFlow();
        });
        builder.addMethod(MethodSpec.methodBuilder("unwrap")
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember(
                "value", CodeBlock.builder().add("$S", "unchecked").build()
            ).build())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addTypeVariable(TypeVariableName.get(
                "E",
                ParameterizedTypeName.get(ClassName.get(Enum.class), TypeVariableName.get("E"))
            ))
            .returns(TypeVariableName.get("E"))
            .addParameter(structure, "value")
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("E")), "type")
            .addCode(unwrap.nextControlFlow("else").addStatement(
                "throw new $T(type.getTypeName() + $S + $T.class.getTypeName())",
                IllegalArgumentException.class,
                " is not enumerated by ",
                structure
            ).endControlFlow().build())
            .build());
        builder.addMethod(MethodSpec.methodBuilder("valueOfName")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(structure)
            .addParameter(String.class, "name")
            .addCode(valueOfName.add("default:\n")
                .indent()
                .addStatement("throw new $T($S + name)", IllegalArgumentException.class, "Not an enumeration name: ")
                .unindent()
                .endControlFlow()
                .build())
            .build());
        consumer.accept(structure, JavaFile.builder(
            structure.packageName(), builder.alwaysQualify(
                compound.getSingulars().stream()
                    .map(singular -> singular.getType().getSimpleName())
                    .distinct()
                    .toArray(String[]::new)
            ).build()
        ).skipJavaLangImports(true).build());

    }
}
