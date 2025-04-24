package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class StructuralTypeModule extends SimpleModule {

    public static final String EXPANSION = "$value", TYPE = "@type", TEMPLATE = "<template>";

    private final String expansion, getter, polymorphism;

    public StructuralTypeModule() {
        this(EXPANSION);
    }

    public StructuralTypeModule(String expansion) {
        this(expansion, "get");
    }

    public StructuralTypeModule(String expansion, String getter) {
        this(expansion, getter, null);
    }

    private StructuralTypeModule(String expansion, String getter, String polymorphism) {
        this.expansion = expansion;
        this.getter = getter;
        this.polymorphism = polymorphism;
    }

    public StructuralTypeModule withPolymorphism() {
        return withPolymorphism(TYPE);
    }

    public StructuralTypeModule withPolymorphism(String polymorphism) {
        return new StructuralTypeModule(expansion, getter, polymorphism);
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.appendAnnotationIntrospector(new StructuralTypeAnnotationIntrospector(expansion, getter, polymorphism));
    }
}
