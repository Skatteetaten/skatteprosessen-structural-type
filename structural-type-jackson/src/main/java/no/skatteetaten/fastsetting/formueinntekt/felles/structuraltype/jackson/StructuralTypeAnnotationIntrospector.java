package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.*;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class StructuralTypeAnnotationIntrospector extends NopAnnotationIntrospector {

    private final String expansion, getter, polymorphism;

    StructuralTypeAnnotationIntrospector(String expansion, String getter, String polymorphism) {
        this.expansion = expansion;
        this.getter = getter;
        this.polymorphism = polymorphism;
    }

    @Override
    public JavaType refineDeserializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
        if (polymorphism == null) {
            TemplatedBy templatedBy = a.getAnnotation(TemplatedBy.class);
            if (templatedBy != null) {
                return config.constructType(templatedBy.value());
            }
        }
        return super.refineDeserializationType(config, a, baseType);
    }

    @Override
    public JavaType refineSerializationType(MapperConfig<?> config, Annotated a, JavaType baseType) throws JsonMappingException {
        DelegationOf delegationOf = a.getAnnotation(DelegationOf.class);
        if (delegationOf != null) {
            return config.constructType(delegationOf.value());
        }
        TemplateOf templateOf = a.getAnnotation(TemplateOf.class);
        if (templateOf != null) {
            return config.constructType(templateOf.value());
        }
        return super.refineSerializationType(config, a, baseType);
    }

    @Override
    public String findImplicitPropertyName(AnnotatedMember member) {
        if (isExpansionProperty(member) || isTemplateConstructorParameter(member)) {
            return expansion;
        }
        return super.findImplicitPropertyName(member);
    }

    @Override
    public Integer findPropertyIndex(Annotated ann) {
        if (ann instanceof AnnotatedMember && isExpansionProperty((AnnotatedMember) ann)) {
            return 0;
        }
        return super.findPropertyIndex(ann);
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        if (a instanceof AnnotatedConstructor && ((AnnotatedConstructor) a).getDeclaringClass().isAnnotationPresent(TemplateOf.class)) {
            return JsonCreator.Mode.PROPERTIES;
        }
        return super.findCreatorAnnotation(config, a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        if (isStructuralSetter(a)) {
            return PropertyName.USE_DEFAULT;
        }
        return super.findNameForDeserialization(a);
    }

    @Override
    public JsonSetter.Value findSetterInfo(Annotated a) {
        if (isStructuralSetter(a)) {
            return JsonSetter.Value.construct(Nulls.DEFAULT, Nulls.DEFAULT);
        }
        return super.findSetterInfo(a);
    }

    @Override
    public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
        if (isStructuralSetter(setter1)) {
            if (!isStructuralSetter(setter2)) {
                return setter1;
            }
        } else if (isStructuralSetter(setter2)) {
            return setter2;
        }
        return super.resolveSetterConflict(config, setter1, setter2);
    }

    private static boolean isStructuralSetter(Annotated a) {
        return a instanceof AnnotatedMethod
                && isStructuralType(((AnnotatedMember) a).getDeclaringClass())
                && ((AnnotatedMethod) a).getParameterCount() == 1
                && isStructuralType(((AnnotatedMethod) a).getRawParameterType(0));
    }

    private boolean isExpansionProperty(AnnotatedMember member) {
        return member.getName().equals(getter) && isStructuralType(member.getMember().getDeclaringClass());
    }

    private static boolean isStructuralType(Class<?> type) {
        return type.isAnnotationPresent(CompoundOf.class)
                || type.isAnnotationPresent(DelegationOf.class)
                || type.isAnnotationPresent(TemplateOf.class)
                || type.isAnnotationPresent(ExpansionOf.class);
    }

    private static boolean isTemplateConstructorParameter(AnnotatedMember parameter) {
        return parameter instanceof AnnotatedParameter
                && parameter.getMember() instanceof Constructor<?>
                && parameter.getDeclaringClass().isAnnotationPresent(TemplateOf.class);
    }

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass, Enum<?>[] enumValues, String[] names) {
        Map<String, String> namedEnumerations = new HashMap<>();
        for (AnnotatedField field : annotatedClass.fields()) {
            EnumeratedAs enumeratedAs = field.getAnnotation(EnumeratedAs.class);
            if (enumeratedAs != null) {
                namedEnumerations.put(field.getName(), enumeratedAs.value());
            }
        }
        for (int index = 0; index < enumValues.length; index++) {
            String override = namedEnumerations.get(enumValues[index].name());
            if (override != null) {
                names[index] = override;
            }
        }
        return names;
    }

    @Override
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
        if (polymorphism != null && ac.hasAnnotation(CompoundOf.class)) {
            Map<String, Class<?>> values = ac.hasAnnotation(DelegatedBy.class) ? Arrays.stream(ac.getAnnotation(DelegatedBy.class).value()).collect(Collectors.toMap(
                    type -> {
                        ProjectionOf projectionOf = type.getAnnotation(ProjectionOf.class);
                        if (projectionOf != null) {
                            return projectionOf.value().getTypeName();
                        }
                        ExpansionOf expansionOf = type.getAnnotation(ExpansionOf.class);
                        if (expansionOf != null) {
                            return expansionOf.value().getTypeName();
                        }
                        throw new IllegalStateException("Unexpected delegation: " + type.getTypeName());
                    },
                    Function.identity()
            )) : Collections.emptyMap();
            return new StdTypeResolverBuilder().init(JsonTypeInfo.Value.construct(
                    JsonTypeInfo.Id.CUSTOM,
                    JsonTypeInfo.As.PROPERTY,
                    polymorphism,
                    ac.hasAnnotation(TemplatedBy.class) ? ac.getAnnotation(TemplatedBy.class).value() : null,
                    false,
                    false
            ), new TypeIdResolverBase() {
                @Override
                public String idFromValue(Object value) {
                    ProjectionOf projectionOf = value.getClass().getAnnotation(ProjectionOf.class);
                    if (projectionOf != null) {
                        return projectionOf.value().getTypeName();
                    }
                    ExpansionOf expansionOf = value.getClass().getAnnotation(ExpansionOf.class);
                    if (expansionOf != null) {
                        return expansionOf.value().getTypeName();
                    }
                    TemplateOf templateOf = value.getClass().getAnnotation(TemplateOf.class);
                    if (templateOf != null) {
                        return StructuralTypeModule.TEMPLATE;
                    }
                    throw new IllegalStateException("Unknown template instance: " + value);
                }

                @Override
                public String idFromValueAndType(Object value, Class<?> suggestedType) {
                    return idFromValue(value);
                }

                @Override
                public JavaType typeFromId(DatabindContext context, String id) {
                    if (id.equals(StructuralTypeModule.TEMPLATE)) {
                        return context.constructType(ac.getAnnotation(TemplatedBy.class).value());
                    }
                    Class<?> type = values.get(id);
                    if (type == null) {
                        throw new IllegalStateException("Cannot resolve type id: " + id + " for " + baseType);
                    }
                    return context.constructType(type);
                }

                @Override
                public JsonTypeInfo.Id getMechanism() {
                    return JsonTypeInfo.Id.CUSTOM;
                }
            });
        }
        return super.findTypeResolver(config, ac, baseType);
    }
}
