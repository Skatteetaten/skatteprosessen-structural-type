package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample.ExpansionStructure;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample.SetterFieldStructure;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample.SimpleStructure;
import org.junit.Before;
import org.junit.Test;

public class StructuralTypeModuleTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper().registerModule(new StructuralTypeModule().withPolymorphism());
    }

    @Test
    public void can_read_structure_as_template() throws Exception {
        SimpleStructure structure = objectMapper.readValue("{\"value\":\"foo\"}", SimpleStructure.class);
        assertThat(structure).isInstanceOf(SimpleStructure.Template.class);
        assertThat(structure.getValue()).isEqualTo("foo");
    }

    @Test
    public void can_read_expansion_property() throws Exception {
        ExpansionStructure structure = objectMapper.readValue("{\"nested\":{\"$value\":\"foo\"}}", ExpansionStructure.class);
        assertThat(structure).isInstanceOf(ExpansionStructure.Template.class);
        assertThat(structure.getNested()).isInstanceOf(ExpansionStructure.NestedTemplate.class);
        assertThat(structure.getNested().get()).isEqualTo("foo");
    }

    @Test
    public void can_read_nested_property() throws Exception {
        ExpansionStructure structure = objectMapper.readValue("{\"nested\":{\"value\":\"foo\"}}", ExpansionStructure.class);
        assertThat(structure).isInstanceOf(ExpansionStructure.Template.class);
        assertThat(structure.getNested()).isInstanceOf(ExpansionStructure.NestedTemplate.class);
        assertThat(structure.getNested().getValue()).isEqualTo("foo");
    }

    @Test
    public void can_read_nested_property_explicit_null_expansion() throws Exception {
        ExpansionStructure structure = objectMapper.readValue("{\"nested\":{\"$value\":null,\"value\":\"foo\"}}", ExpansionStructure.class);
        assertThat(structure).isInstanceOf(ExpansionStructure.Template.class);
        assertThat(structure.getNested()).isInstanceOf(ExpansionStructure.NestedTemplate.class);
        assertThat(structure.getNested().getValue()).isEqualTo("foo");
    }

    @Test
    public void can_read_nested_property_explicit_dual_expansion() throws Exception {
        ExpansionStructure structure = objectMapper.readValue("{\"nested\":{\"$value\":\"foo\",\"value\":\"bar\"}}", ExpansionStructure.class);
        assertThat(structure).isInstanceOf(ExpansionStructure.Template.class);
        assertThat(structure.getNested()).isInstanceOf(ExpansionStructure.NestedTemplate.class);
        assertThat(structure.getNested().get()).isEqualTo("foo");
        assertThat(structure.getNested().getValue()).isEqualTo("bar");
    }

    @Test
    public void can_write_expansion_property() throws Exception {
        ExpansionStructure structure = new ExpansionStructure.Template();
        structure.setNested("foo");
        assertThat(objectMapper.writeValueAsString(structure)).isEqualTo("{\"@type\":\"<template>\",\"nested\":{"
            + "\"@type\":\"" + String.class.getTypeName() + "\","
            + "\"$value\":\"foo\","
            + "\"value\":null}}");
    }

    @Test
    public void can_write_expansion_property_template() throws Exception {
        ExpansionStructure structure = new ExpansionStructure.Template();
        structure.setNested(new ExpansionStructure.NestedTemplate("foo"));
        assertThat(objectMapper.writeValueAsString(structure)).isEqualTo("{\"@type\":\"<template>\",\"nested\":{"
            + "\"@type\":\"<template>\","
            + "\"$value\":\"foo\","
            + "\"value\":null}}");
    }

    @Test
    public void can_write_nested_property() throws Exception {
        ExpansionStructure structure = new ExpansionStructure.Template();
        structure.setNested(new ExpansionStructure.NestedTemplate());
        structure.getNested().setValue("foo");
        assertThat(objectMapper.writeValueAsString(structure)).isEqualTo("{\"@type\":\"<template>\",\"nested\":{"
            + "\"@type\":\"<template>\","
            + "\"$value\":null,"
            + "\"value\":\"foo\"}}");
    }

    @Test
    public void can_serialize_projection() throws Exception {
        SetterFieldStructure.Wrapper wrapper = new SetterFieldStructure.Wrapper();
        wrapper.getOuter().getInner().setValue("foo");
        assertThat(objectMapper.writeValueAsString(wrapper)).isEqualTo("{\"outer\":{"
            + "\"@type\":\"" + SetterFieldStructure.OuterDelegate.class.getTypeName() + "\","
            + "\"inner\":{\"@type\":\"" + SimpleStructure.Delegate.class.getTypeName() + "\",\"value\":\"foo\"}}}");
    }

    @Test
    public void can_read_structure_as_projection() throws Exception {
        SimpleStructure structure = objectMapper.readValue("{\"@type\":\""
                + SimpleStructure.Delegate.class.getTypeName()
                + "\",\"value\":\"foo\"}", SimpleStructure.class);
        assertThat(structure).isInstanceOf(SimpleStructure.Projection.class);
        assertThat(structure.getValue()).isEqualTo("foo");
    }

    @Test
    public void can_read_structure_as_template_explicit() throws Exception {
        SimpleStructure structure = objectMapper.readValue("{\"@type\":\""
                + "<template>"
                + "\",\"value\":\"foo\"}", SimpleStructure.class);
        assertThat(structure).isInstanceOf(SimpleStructure.Template.class);
        assertThat(structure.getValue()).isEqualTo("foo");
    }
}
