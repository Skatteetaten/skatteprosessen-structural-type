package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.*;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.sample.*;
import org.junit.Before;
import org.junit.Test;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

public class StructuralTypeTest {

    private Function<Map<ClassName, JavaFile>, List<Class<?>>> compiler;

    @Before
    public void setUp() {
        compiler = new InMemoryCompiler();
    }

    @Test
    public void can_resolve_missing_and_single_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, "nothing");
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_missing_and_optional_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafFoo.class
        )).make(
            SampleTypedLeafFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, "nothing");
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_missing_and_list_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafOtherListFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleTypedLeafOtherListFoo foo = new SampleTypedLeafOtherListFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.getFoo().add("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "bar");
        assertThat(foo.getFoo()).containsExactly("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add("nothing");
        assertThat(getList("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleTypedLeafOtherListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_single_and_single_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", fooProjection)).isNull();
        foo.setFoo("foo");
        assertThat(get("getFoo", fooProjection)).isEqualTo("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherFoo other = new SampleTypedLeafOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();
        other.setFoo("foo");
        assertThat(get("getFoo", otherProjection)).isEqualTo("foo");
        set("setFoo", otherProjection, "qux");
        assertThat(other.getFoo()).isEqualTo("qux");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, "foo");
        assertThat(get("getFoo", template)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_optional_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafOtherFoo.class
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherFoo other = new SampleTypedLeafOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        other.setFoo("foo");
        assertThat(getOptional("getFoo", otherProjection)).contains("foo");
        set("setFoo", otherProjection, "qux");
        assertThat(other.getFoo()).isEqualTo("qux");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_optional_and_optional_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> true
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherFoo other = new SampleTypedLeafOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        other.setFoo("foo");
        assertThat(getOptional("getFoo", otherProjection)).contains("foo");
        set("setFoo", otherProjection, "qux");
        assertThat(other.getFoo()).isEqualTo("qux");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_single_and_list_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).add("nothing");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).remove(0);
        assertThat(foo.getFoo()).isNull();
        assertThat(getList("getFoo", fooProjection)).isEmpty();

        SampleTypedLeafOtherListFoo other = new SampleTypedLeafOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add("foo");
        assertThat(getList("getFoo", otherProjection)).containsExactly("foo");
        getList("getFoo", otherProjection).set(0, "qux");
        assertThat(other.getFoo()).containsExactly("qux");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux");
        getList("getFoo", otherProjection).add("baz");
        assertThat(other.getFoo()).containsExactly("qux", "baz");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux", "baz");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_optional_and_list_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafFoo.class
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).add("nothing");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).remove(0);
        assertThat(foo.getFoo()).isNull();
        assertThat(getList("getFoo", fooProjection)).isEmpty();

        SampleTypedLeafOtherListFoo other = new SampleTypedLeafOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add("foo");
        assertThat(getList("getFoo", otherProjection)).containsExactly("foo");
        getList("getFoo", otherProjection).set(0, "qux");
        assertThat(other.getFoo()).containsExactly("qux");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux");
        getList("getFoo", otherProjection).add("baz");
        assertThat(other.getFoo()).containsExactly("qux", "baz");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux", "baz");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_list_and_list_typed_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafListFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleTypedLeafListFoo foo = new SampleTypedLeafListFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.getFoo().add("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "qux");
        assertThat(foo.getFoo()).containsExactly("qux");
        assertThat(getList("getFoo", fooProjection)).containsExactly("qux");
        getList("getFoo", fooProjection).add("baz");
        assertThat(foo.getFoo()).containsExactly("qux", "baz");
        assertThat(getList("getFoo", fooProjection)).containsExactly("qux", "baz");

        SampleTypedLeafOtherListFoo other = new SampleTypedLeafOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add("foo");
        assertThat(getList("getFoo", otherProjection)).containsExactly("foo");
        getList("getFoo", otherProjection).set(0, "qux");
        assertThat(other.getFoo()).containsExactly("qux");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux");
        getList("getFoo", otherProjection).add("baz");
        assertThat(other.getFoo()).containsExactly("qux", "baz");
        assertThat(getList("getFoo", otherProjection)).containsExactly("qux", "baz");

        Object template = templateInstanceOf(types, SampleTypedLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_single_and_single_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherIntegerFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", fooProjection)).isNull();
        foo.setFoo("foo");
        assertThat(get("getFoo", fooProjection)).isEqualTo("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherIntegerFoo other = new SampleTypedLeafOtherIntegerFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();
        other.setFoo(42);
        assertThat(get("getFoo", otherProjection)).isEqualTo("42");
        set("setFoo", otherProjection, "84");
        assertThat(other.getFoo()).isEqualTo(84);

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, "foo");
        assertThat(get("getFoo", template)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_optional_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafOtherIntegerFoo.class
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherIntegerFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherIntegerFoo other = new SampleTypedLeafOtherIntegerFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        other.setFoo(42);
        assertThat(getOptional("getFoo", otherProjection)).contains("42");
        set("setFoo", otherProjection, "84");
        assertThat(other.getFoo()).isEqualTo(84);

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_optional_and_optional_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> true
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherIntegerFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getOptional("getFoo", fooProjection)).contains("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafOtherIntegerFoo other = new SampleTypedLeafOtherIntegerFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        other.setFoo(42);
        assertThat(getOptional("getFoo", otherProjection)).contains("42");
        set("setFoo", otherProjection, "84");
        assertThat(other.getFoo()).isEqualTo(84);

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, "foo");
        assertThat(getOptional("getFoo", template)).contains("foo");
    }

    @Test
    public void can_resolve_single_and_list_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherIntegerListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).add("nothing");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).remove(0);
        assertThat(foo.getFoo()).isNull();
        assertThat(getList("getFoo", fooProjection)).isEmpty();

        SampleTypedLeafOtherIntegerListFoo other = new SampleTypedLeafOtherIntegerListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add(42);
        assertThat(getList("getFoo", otherProjection)).containsExactly("42");
        getList("getFoo", otherProjection).set(0, "21");
        assertThat(other.getFoo()).containsExactly(21);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21");
        getList("getFoo", otherProjection).add("84");
        assertThat(other.getFoo()).containsExactly(21, 84);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21", "84");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_optional_and_list_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafFoo.class
        )).make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafOtherIntegerListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.setFoo("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).add("nothing");
        assertThat(foo.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", fooProjection)).containsExactly("bar");
        getList("getFoo", fooProjection).remove(0);
        assertThat(foo.getFoo()).isNull();
        assertThat(getList("getFoo", fooProjection)).isEmpty();

        SampleTypedLeafOtherIntegerListFoo other = new SampleTypedLeafOtherIntegerListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add(42);
        assertThat(getList("getFoo", otherProjection)).containsExactly("42");
        getList("getFoo", otherProjection).set(0, "21");
        assertThat(other.getFoo()).containsExactly(21);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21");
        getList("getFoo", otherProjection).add("84");
        assertThat(other.getFoo()).containsExactly(21, 84);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21", "84");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_single_and_single_typed_leaf_renamed() {
        List<Class<?>> types = compiler.apply(new StructuralType().withNodeResolver((type, property) -> "foo").make(
            SampleTypedLeafFoo.class,
            SampleTypedLeafBar.class
        ));

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafBar.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafBar.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafBar.class)))
            .isPublic();

        SampleTypedLeafFoo foo = new SampleTypedLeafFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", fooProjection)).isNull();
        foo.setFoo("foo");
        assertThat(get("getFoo", fooProjection)).isEqualTo("foo");
        set("setFoo", fooProjection, "bar");
        assertThat(foo.getFoo()).isEqualTo("bar");

        SampleTypedLeafBar other = new SampleTypedLeafBar();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();
        other.setBar("foo");
        assertThat(get("getFoo", otherProjection)).isEqualTo("foo");
        set("setFoo", otherProjection, "qux");
        assertThat(other.getBar()).isEqualTo("qux");

        Object template = templateInstanceOf(types, SampleTypedLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, "foo");
        assertThat(get("getFoo", template)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_missing_and_single_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumLeafFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleEnumLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR")
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.FOO);
        leaf.setFoo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleEnumLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", template))
            .containsSame(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_missing_and_optional_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleEnumLeafFoo.class
        )).make(
            SampleEnumLeafFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleEnumLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR")
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.FOO);
        leaf.setFoo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleEnumLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", template))
            .containsSame(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_missing_and_list_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumLeafListFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleEnumLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR")
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.FOO);
        leaf.getFoo().add(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getList("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleEnumLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getList("getFoo", template))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_single_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumLeafFoo.class,
            SampleEnumLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleEnumLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumOtherFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafOtherFoo.class)))
            .isPublic();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", leafProjection)).isNull();
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.FOO);
        leaf.setFoo(SampleEnumFoo.BAR);
        assertThat(get("getFoo", leafProjection)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(leaf.getFoo()).isNull();

        SampleEnumLeafOtherFoo other = new SampleEnumLeafOtherFoo();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", emptyProjection)).isNull();
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.FOO);
        other.setFoo(SampleEnumOtherFoo.QUX);
        assertThat(get("getFoo", emptyProjection)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(other.getFoo()).isNull();

        Object template = templateInstanceOf(types, SampleEnumLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(get("getFoo", template)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_optional_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleEnumLeafFoo.class
        )).make(
            SampleEnumLeafFoo.class,
            SampleEnumLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleEnumLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumOtherFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafOtherFoo.class)))
            .isPublic();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.FOO);
        leaf.setFoo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(leaf.getFoo()).isNull();

        SampleEnumLeafOtherFoo other = new SampleEnumLeafOtherFoo();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.FOO);
        other.setFoo(SampleEnumOtherFoo.QUX);
        assertThat(getOptional("getFoo", emptyProjection))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(other.getFoo()).isNull();

        Object template = templateInstanceOf(types, SampleEnumLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", template)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_list_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumLeafListFoo.class,
            SampleEnumLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleEnumLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumOtherFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafOtherFoo.class)))
            .isPublic();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.FOO);
        leaf.getFoo().add(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEnumLeafOtherFoo other = new SampleEnumLeafOtherFoo();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.FOO);
        other.setFoo(SampleEnumOtherFoo.QUX);
        assertThat(getList("getFoo", emptyProjection))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.QUX);

        Object template = templateInstanceOf(types, SampleEnumLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getList("getFoo", template))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(getList("getFoo", template)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_optional_enum_leaf_and_list_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleEnumLeafOtherFoo.class
        )).make(
            SampleEnumLeafListFoo.class,
            SampleEnumLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleEnumLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumOtherFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafOtherFoo.class)))
            .isPublic();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.FOO);
        leaf.getFoo().add(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEnumLeafOtherFoo other = new SampleEnumLeafOtherFoo();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.FOO);
        other.setFoo(SampleEnumOtherFoo.QUX);
        assertThat(getList("getFoo", emptyProjection))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(other.getFoo()).isEqualTo(SampleEnumOtherFoo.QUX);

        Object template = templateInstanceOf(types, SampleEnumLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getList("getFoo", template))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(getList("getFoo", template)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_list_enum_leaf_and_list_enum_leaf() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumLeafListFoo.class,
            SampleEnumLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleEnumLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumOtherFoo.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafOtherListFoo.class)))
            .isPublic();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.FOO);
        leaf.getFoo().add(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        getList("getFoo", leafProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(getList("getFoo", leafProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));

        SampleEnumLeafOtherListFoo other = new SampleEnumLeafOtherListFoo();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getFoo()).containsExactly(SampleEnumOtherFoo.FOO);
        other.getFoo().add(SampleEnumOtherFoo.QUX);
        assertThat(getList("getFoo", emptyProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        getList("getFoo", emptyProjection).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(getList("getFoo", emptyProjection)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));

        Object template = templateInstanceOf(types, SampleEnumLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getList("getFoo", template))
            .containsExactly(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        getList("getFoo", template).add(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(getList("getFoo", template)).containsExactly(
            enumerationConstantOf(types, SampleEnumFoo.class, "FOO"),
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_single_enum_leaf_renamed() {
        List<Class<?>> types = compiler.apply(new StructuralType().withNodeResolver((type, property) -> {
            if (type.isEnum()) {
                return property.equals("BAZ") ? "FOO" : property;
            } else {
                return "foo";
            }
        }).make(
            SampleEnumLeafFoo.class,
            SampleEnumLeafBar.class
        ));

        assertThat(structureOf(types, SampleEnumLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafBar.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(enumerationOf(types, SampleEnumFoo.class))
            .isEqualTo(enumerationOf(types, SampleEnumBar.class))
            .hasOnlyDeclaredFields("FOO", "BAR", "QUX")
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafBar.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafBar.class)))
            .isPublic();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", leafProjection)).isNull();
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.FOO);
        leaf.setFoo(SampleEnumFoo.BAR);
        assertThat(get("getFoo", leafProjection)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        set("setFoo", leafProjection, enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        assertThat(leaf.getFoo()).isNull();

        SampleEnumLeafBar other = new SampleEnumLeafBar();
        Object emptyProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", emptyProjection)).isNull();
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(other.getBar()).isEqualTo(SampleEnumBar.BAZ);
        other.setBar(SampleEnumBar.QUX);
        assertThat(get("getFoo", emptyProjection)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "QUX"));
        set("setFoo", emptyProjection, enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(other.getBar()).isNull();

        Object template = templateInstanceOf(types, SampleEnumLeafFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(get("getFoo", template)).isEqualTo(enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
    }

    @Test
    public void can_resolve_missing_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, templateInstanceOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).containsSame(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_missing_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, templateInstanceOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).containsSame(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_missing_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getList("getFoo", emptyProjection)).isEmpty();
        getList("getFoo", emptyProjection).add(templateInstanceOf(types, SampleTypedLeafFoo.class));
        assertThat(getList("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleBranchOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", branchProjection)).isNull();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(get("getFoo", branchProjection)).isInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherFoo other = new SampleBranchOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", otherProjection, otherFooProjection);
        assertThat(get("getFoo", otherProjection)).isInstanceOf(
            projectionOf(types, SampleTypedLeafOtherFoo.class)
        );
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(get("getFoo", template)).isEqualTo(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchOtherFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleBranchOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherFoo other = new SampleBranchOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", otherProjection, otherFooProjection);
        assertThat(getOptional("getFoo", otherProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafOtherFoo.class)
        );
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).containsSame(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_optional_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchOtherFoo.class
                || property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleBranchOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherFoo other = new SampleBranchOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", otherProjection)).isEmpty();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", otherProjection, otherFooProjection);
        assertThat(getOptional("getFoo", otherProjection)).containsInstanceOf(
            projectionOf(types, SampleTypedLeafOtherFoo.class)
        );
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).containsSame(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleBranchOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherListFoo other = new SampleBranchOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", otherProjection).add(otherFooProjection);
        assertThat(getList("getFoo", otherProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafOtherFoo.class)));
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_optional_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleBranchOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherListFoo other = new SampleBranchOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", otherProjection).add(otherFooProjection);
        assertThat(getList("getFoo", otherProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafOtherFoo.class)));
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_list_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleBranchOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleBranchOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchOtherListFoo other = new SampleBranchOtherListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        SampleTypedLeafOtherFoo otherFoo = new SampleTypedLeafOtherFoo();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", otherProjection).add(otherFooProjection);
        assertThat(getList("getFoo", otherProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafOtherFoo.class)));
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setFoo("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getFoo()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_typed_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", branchProjection)).isNull();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(get("getFoo", branchProjection)).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", leafProjection)).isNull();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(get("getFoo", leafProjection))
            .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .satisfies(value -> assertThat(getOptional("get", value)).contains("bar"));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(get("getFoo", template)).isEqualTo(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateExpansion)).isNull();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(get("getFoo", templateExpansion)).isEqualTo(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_single_typed_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value)).contains("bar"));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_optional_typed_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafOtherFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value)).contains("bar"));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_optional_typed_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> true
        )).make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value)).contains("bar"));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_single_typed_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
                .satisfies(value -> assertThat(getOptional("get", value)).contains("bar")));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_optional_typed_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleTypedLeafOtherFoo.class
        )).make(
            SampleBranchListFoo.class,
            SampleTypedLeafOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherFoo leaf = new SampleTypedLeafOtherFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo("bar");
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
                .satisfies(value -> assertThat(getOptional("get", value)).contains("bar")));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_list_typed_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherListFoo leaf = new SampleTypedLeafOtherListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly("bar");
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
                .satisfies(value -> assertThat(getOptional("get", value)).contains("bar")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_list_typed_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherListFoo leaf = new SampleTypedLeafOtherListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly("bar");
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
                .satisfies(value -> assertThat(getOptional("get", value)).contains("bar")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_list_typed_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleTypedLeafOtherListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, String.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherListFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleTypedLeafOtherListFoo leaf = new SampleTypedLeafOtherListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(types, SampleTypedLeafFoo.class, String.class, "bar");
        assertThat(getOptional("get", expansionFoo)).contains("bar");
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly("bar");
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, String.class))
                .satisfies(value -> assertThat(getOptional("get", value)).contains("bar")));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(types, SampleTypedLeafFoo.class, "bar");
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate)).contains("bar");
    }

    @Test
    public void can_resolve_single_enum_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", branchProjection)).isNull();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(get("getFoo", branchProjection)).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", leafProjection)).isNull();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(get("getFoo", leafProjection))
            .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .satisfies(value -> assertThat(getOptional("get", value))
                .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(get("getFoo", template)).isEqualTo(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateExpansion)).isNull();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(get("getFoo", templateExpansion)).isEqualTo(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value))
                .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_optional_enum_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleEnumLeafFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value))
                .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_optional_enum_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> true
        )).make(
            SampleBranchFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(getOptional("getFoo", branchProjection))
            .containsInstanceOf(projectionOf(types, SampleTypedLeafFoo.class));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", leafProjection, expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(getOptional("getFoo", leafProjection))
            .containsInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasValueSatisfying(value -> assertThat(getOptional("get", value))
                .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR")));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(getOptional("getFoo", template)).contains(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        set("setFoo", templateExpansion, templateExpansionDelegate);
        assertThat(getOptional("getFoo", templateExpansion)).contains(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_single_enum_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
                .satisfies(value -> assertThat(getOptional("get", value))
                    .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"))));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_optional_enum_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleEnumLeafFoo.class
        )).make(
            SampleBranchListFoo.class,
            SampleEnumLeafFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafFoo leaf = new SampleEnumLeafFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).isEqualTo(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
                .satisfies(value -> assertThat(getOptional("get", value))
                    .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"))));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_list_enum_leaf_and_single_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchFoo.class,
            SampleEnumLeafListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
                .satisfies(value -> assertThat(getOptional("get", value))
                    .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"))));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_list_enum_leaf_and_optional_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(new SimpleStructuralResolver(
            property -> property.getDeclaringClass() == SampleBranchFoo.class
        )).make(
            SampleBranchFoo.class,
            SampleEnumLeafListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
                .satisfies(value -> assertThat(getOptional("get", value))
                    .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"))));

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_list_enum_leaf_and_list_branch() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchListFoo.class,
            SampleEnumLeafListFoo.class
        ));

        assertThat(structureOf(types, SampleBranchListFoo.class))
            .isEqualTo(structureOf(types, SampleEnumLeafListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo", "value")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumLeafListFoo.class)))
            .isPublic();

        SampleBranchListFoo branch = new SampleBranchListFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", branchProjection)).isEmpty();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", branchProjection).add(branchFooProjection);
        assertThat(getList("getFoo", branchProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element).isInstanceOf(projectionOf(types, SampleTypedLeafFoo.class)));
        assertThat(getOptional("getFoo", branchFooProjection)).isEmpty();
        branchFoo.setFoo("foo");
        assertThat(getOptional("getFoo", branchFooProjection)).contains("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");
        assertThat(has("has", branchFooProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("get", branchFooProjection)).isEmpty();

        SampleEnumLeafListFoo leaf = new SampleEnumLeafListFoo();
        Object leafProjection = projectionInstanceOf(types, leaf);
        assertThat(has("hasFoo", leafProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", leafProjection)).isEmpty();
        Object expansionFoo = expansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            SampleEnumFoo.class, SampleEnumFoo.BAR
        );
        assertThat(getOptional("get", expansionFoo)).contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
        assertThat(has("has", expansionFoo)).isEqualTo(PropertyDefinition.SINGLE);
        getList("getFoo", leafProjection).add(expansionFoo);
        assertThat(leaf.getFoo()).containsExactly(SampleEnumFoo.BAR);
        assertThat(getList("getFoo", leafProjection))
            .hasSize(1)
            .allSatisfy(element -> assertThat(element)
                .isInstanceOf(expansionOf(types, SampleTypedLeafFoo.class, SampleEnumFoo.class))
                .satisfies(value -> assertThat(getOptional("get", value))
                    .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"))));

        Object template = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        getList("getFoo", template).add(templateDelegate);
        assertThat(getList("getFoo", template)).containsExactly(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateDelegate)).isEmpty();
        set("setFoo", templateDelegate, "foo");
        assertThat(getOptional("getFoo", templateDelegate)).contains("foo");
        assertThat(has("has", templateDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateDelegate)).isEmpty();

        Object templateExpansion = templateInstanceOf(types, SampleBranchListFoo.class);
        assertThat(has("hasFoo", templateExpansion)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", templateExpansion)).isEmpty();
        Object templateExpansionDelegate = templateExpansionInstanceOf(
            types, SampleTypedLeafFoo.class,
            enumerationConstantOf(types, SampleEnumFoo.class, "BAR")
        );
        getList("getFoo", templateExpansion).add(templateExpansionDelegate);
        assertThat(getList("getFoo", templateExpansion)).containsExactly(templateExpansionDelegate);
        assertThat(has("hasFoo", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", templateExpansionDelegate)).isEmpty();
        set("setFoo", templateExpansionDelegate, "foo");
        assertThat(getOptional("getFoo", templateExpansionDelegate)).contains("foo");
        assertThat(has("has", templateExpansionDelegate)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("get", templateExpansionDelegate))
            .contains(enumerationConstantOf(types, SampleEnumFoo.class, "BAR"));
    }

    @Test
    public void can_resolve_list_and_list_typed_leaf_conversion() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafListFoo.class,
            SampleTypedLeafOtherIntegerListFoo.class
        ));

        assertThat(structureOf(types, SampleTypedLeafListFoo.class))
            .isEqualTo(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasDeclaredMethods("getFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafListFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafListFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafOtherIntegerListFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafOtherIntegerListFoo.class)))
            .isPublic();

        SampleTypedLeafListFoo foo = new SampleTypedLeafListFoo();
        Object fooProjection = projectionInstanceOf(types, foo);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", fooProjection)).isEmpty();
        foo.getFoo().add("foo");
        assertThat(getList("getFoo", fooProjection)).containsExactly("foo");
        getList("getFoo", fooProjection).set(0, "qux");
        assertThat(foo.getFoo()).containsExactly("qux");
        assertThat(getList("getFoo", fooProjection)).containsExactly("qux");
        getList("getFoo", fooProjection).add("baz");
        assertThat(foo.getFoo()).containsExactly("qux", "baz");
        assertThat(getList("getFoo", fooProjection)).containsExactly("qux", "baz");

        SampleTypedLeafOtherIntegerListFoo other = new SampleTypedLeafOtherIntegerListFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", otherProjection)).isEmpty();
        other.getFoo().add(42);
        assertThat(getList("getFoo", otherProjection)).containsExactly("42");
        getList("getFoo", otherProjection).set(0, "21");
        assertThat(other.getFoo()).containsExactly(21);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21");
        getList("getFoo", otherProjection).add("84");
        assertThat(other.getFoo()).containsExactly(21, 84);
        assertThat(getList("getFoo", otherProjection)).containsExactly("21", "84");

        Object template = templateInstanceOf(types, SampleTypedLeafListFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.LIST);
        assertThat(getList("getFoo", template)).isEmpty();
        getList("getFoo", template).add("foo");
        assertThat(getList("getFoo", template)).containsExactly("foo");
    }

    @Test
    public void can_resolve_single_and_single_branch_renamed() {
        List<Class<?>> types = compiler.apply(new StructuralType().withNodeResolver((type, property) -> "foo").make(
            SampleBranchFoo.class,
            SampleBranchBar.class
        ));

        assertThat(structureOf(types, SampleBranchFoo.class))
            .isEqualTo(structureOf(types, SampleBranchBar.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchBar.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchBar.class)))
            .isPublic();

        assertThat(structureOf(types, SampleTypedLeafFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafFoo.class)))
            .isPublic();

        SampleBranchFoo branch = new SampleBranchFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", branchProjection)).isNull();
        SampleTypedLeafFoo branchFoo = new SampleTypedLeafFoo();
        Object branchFooProjection = projectionInstanceOf(types, branchFoo);
        assertThat(has("hasFoo", branchFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", branchProjection, branchFooProjection);
        assertThat(get("getFoo", branchProjection)).isInstanceOf(
            projectionOf(types, SampleTypedLeafFoo.class)
        );
        assertThat(get("getFoo", branchFooProjection)).isNull();
        branchFoo.setFoo("foo");
        assertThat(get("getFoo", branchFooProjection)).isEqualTo("foo");
        set("setFoo", branchFooProjection, "bar");
        assertThat(branchFoo.getFoo()).isEqualTo("bar");

        SampleBranchBar other = new SampleBranchBar();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();
        SampleTypedLeafBar otherFoo = new SampleTypedLeafBar();
        Object otherFooProjection = projectionInstanceOf(types, otherFoo);
        assertThat(has("hasFoo", otherFooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        set("setFoo", otherProjection, otherFooProjection);
        assertThat(get("getFoo", otherProjection)).isInstanceOf(
            projectionOf(types, SampleTypedLeafBar.class)
        );
        assertThat(get("getFoo", otherFooProjection)).isNull();
        otherFoo.setBar("foo");
        assertThat(get("getFoo", otherFooProjection)).isEqualTo("foo");
        set("setFoo", otherFooProjection, "bar");
        assertThat(otherFoo.getBar()).isEqualTo("bar");

        Object template = templateInstanceOf(types, SampleBranchFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        Object templateDelegate = templateInstanceOf(types, SampleTypedLeafFoo.class);
        set("setFoo", template, templateDelegate);
        assertThat(get("getFoo", template)).isEqualTo(templateDelegate);
        assertThat(has("hasFoo", templateDelegate)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", templateDelegate)).isNull();
        set("setFoo", templateDelegate, "foo");
        assertThat(get("getFoo", templateDelegate)).isEqualTo("foo");
    }

    @Test
    public void can_resolve_single_and_single_branch_recursive() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleBranchRecursiveFoo.class,
            SampleBranchRecursiveOtherFoo.class
        ));

        assertThat(structureOf(types, SampleBranchRecursiveFoo.class))
            .isEqualTo(structureOf(types, SampleBranchRecursiveOtherFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleBranchRecursiveFoo.class))
            .isEqualTo(templateOf(types, SampleBranchRecursiveOtherFoo.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchRecursiveFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchRecursiveFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchRecursiveFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleBranchRecursiveOtherFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleBranchRecursiveOtherFoo.class)))
            .isPublic();

        SampleBranchRecursiveFoo branch = new SampleBranchRecursiveFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", branchProjection)).isNull();

        SampleBranchRecursiveOtherFoo other = new SampleBranchRecursiveOtherFoo();
        Object otherProjection = projectionInstanceOf(types, other);
        assertThat(has("hasFoo", otherProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", otherProjection)).isNull();

        set("setFoo", branchProjection, otherProjection);
        assertThat(get("getFoo", branchProjection)).isInstanceOf(
            projectionOf(types, SampleBranchRecursiveOtherFoo.class)
        );

        set("setFoo", otherProjection, branchProjection);
        assertThat(get("getFoo", otherProjection)).isInstanceOf(
            projectionOf(types, SampleBranchRecursiveFoo.class)
        );

        Object template = templateInstanceOf(types, SampleBranchRecursiveFoo.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", template)).isNull();
        set("setFoo", template, template);
        assertThat(get("getFoo", template)).isEqualTo(template);
    }

    @Test
    public void can_resolve_mixed_property_to_minimal_result() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleEnumNestedLeafFoo.class,
            SampleEnumNestedBranchFoo.class
        ));

        assertThat(structureOf(types, SampleEnumNestedLeafFoo.class))
            .isEqualTo(structureOf(types, SampleEnumNestedBranchFoo.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleEnumNestedLeafFoo.class))
            .isEqualTo(templateOf(types, SampleEnumNestedBranchFoo.class))
            .hasOnlyDeclaredFields("foo", "bar")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumNestedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumNestedLeafFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumNestedLeafFoo.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEnumNestedBranchFoo.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEnumNestedLeafFoo.class)))
            .isPublic();

        SampleEnumNestedLeafFoo nested = new SampleEnumNestedLeafFoo();
        Object nestedProjection = projectionInstanceOf(types, nested);
        assertThat(has("hasFoo", nestedProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getFoo", nestedProjection)).isNull();
        assertThat(has("hasBar", nestedProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(get("getBar", nestedProjection)).isNull();

        SampleEnumLeafFoo branch = new SampleEnumLeafFoo();
        Object branchProjection = projectionInstanceOf(types, branch);
        assertThat(has("hasFoo", branchProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", branchProjection)).isEmpty();

        set("setFoo", nestedProjection, branchProjection);
        assertThat(get("getFoo", nestedProjection)).isInstanceOf(
            projectionOf(types, SampleEnumLeafFoo.class)
        );

        set("setFoo", branchProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(getOptional("getFoo", branchProjection)).containsInstanceOf(
            enumerationOf(types, SampleEnumFoo.class)
        );

        set("setBar", nestedProjection, enumerationConstantOf(types, SampleEnumFoo.class, "FOO"));
        assertThat(get("getBar", nestedProjection)).isInstanceOf(
            enumerationOf(types, SampleEnumFoo.class)
        );
    }

    @Test
    public void can_resolve_type_hierarchy() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(type -> {
            if (type.getPackageName().startsWith("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype")) {
                return Optional.of(new StructuralResolver.Branch<Field>() {
                    @Override
                    public Iterable<Field> getProperties() {
                        return Arrays.asList(type.getDeclaredFields());
                    }

                    @Override
                    public String getName(Field field) {
                        return field.getName();
                    }

                    @Override
                    public Class<?> getType(Field field) {
                        return field.getType();
                    }

                    @Override
                    public Optional<Class<?>> getSuperClass() {
                        if (type == SampleSubLeftFoo.class || type == SampleSubRightFoo.class) {
                            return Optional.of(SampleBaseFoo.class);
                        } else if (type == SampleSubLeftBar.class || type == SampleSubRightBar.class) {
                            return Optional.of(SampleBaseBar.class);
                        } else {
                            return Optional.empty();
                        }
                    }

                    @Override
                    public List<Class<?>> getSubClasses() {
                        if (type == SampleBaseFoo.class) {
                            return Arrays.asList(SampleSubLeftFoo.class, SampleSubRightFoo.class);
                        } else if (type == SampleBaseBar.class) {
                            return Arrays.asList(SampleSubLeftBar.class, SampleSubRightBar.class);
                        } else {
                            return Collections.emptyList();
                        }
                    }
                });
            } else {
                return Optional.empty();
            }
        }).withGrouper(new IndexAlignedGrouper()).make(
            SampleBaseFoo.class,
            SampleBaseBar.class
        ));

        assertThat(structureOf(types, SampleBaseFoo.class))
            .isEqualTo(structureOf(types, SampleBaseBar.class))
            .hasDeclaredMethods("getBase", "setBase", "hasBase")
            .isPublic()
            .isInterface();

        assertThat(structureOf(types, SampleSubLeftFoo.class))
            .isEqualTo(structureOf(types, SampleSubLeftBar.class))
            .hasDeclaredMethods("getLeft", "setLeft", "hasLeft")
            .isPublic()
            .isInterface();

        assertThat(structureOf(types, SampleSubRightFoo.class))
            .isEqualTo(structureOf(types, SampleSubRightBar.class))
            .hasDeclaredMethods("getRight", "setRight", "hasRight")
            .isPublic()
            .isInterface();

        assertThat(structureOf(types, SampleBaseFoo.class).getAnnotation(SubtypedBy.class).value()).contains(
            structureOf(types, SampleSubLeftFoo.class),
            structureOf(types, SampleSubRightFoo.class)
        );
    }

    @Test
    public void can_resolve_unbalanced_type_hierarchy() {
        List<Class<?>> types = compiler.apply(new StructuralType().withStructuralResolver(type -> {
            if (type.getPackageName().startsWith("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype")) {
                return Optional.of(new StructuralResolver.Branch<Field>() {
                    @Override
                    public Iterable<Field> getProperties() {
                        return Arrays.asList(type.getDeclaredFields());
                    }

                    @Override
                    public String getName(Field field) {
                        return field.getName();
                    }

                    @Override
                    public Class<?> getType(Field field) {
                        return field.getType();
                    }

                    @Override
                    public Optional<Class<?>> getSuperClass() {
                        if (type == SampleSubLeftFoo.class) {
                            return Optional.of(SampleBaseFoo.class);
                        } else {
                            return Optional.empty();
                        }
                    }

                    @Override
                    public List<Class<?>> getSubClasses() {
                        if (type == SampleBaseFoo.class) {
                            return Collections.singletonList(SampleSubLeftFoo.class);
                        } else {
                            return Collections.emptyList();
                        }
                    }
                });
            } else {
                return Optional.empty();
            }
        }).withGrouper(new IndexAlignedGrouper()).make(
            SampleSubLeftQux.class,
            SampleSubLeftFoo.class
        ));

        assertThat(structureOf(types, SampleSubLeftQux.class))
            .isEqualTo(structureOf(types, SampleSubLeftFoo.class))
            .hasDeclaredMethods("getLeft", "setLeft", "hasLeft")
            .isPublic()
            .isInterface();

        assertThat(structureOf(types, SampleBaseFoo.class))
            .hasDeclaredMethods("getBase", "setBase", "hasBase")
            .isPublic()
            .isInterface();

        assertThat(structureOf(types, SampleBaseFoo.class).getAnnotation(SubtypedBy.class).value()).contains(
            structureOf(types, SampleSubLeftFoo.class)
        );
    }

    @Test
    public void can_resolve_missing_and_single_typed_leaf_primitive() {
        List<Class<?>> types = compiler.apply(new StructuralType().make(
            SampleTypedLeafPrimitive.class,
            SampleEmpty.class
        ));

        assertThat(structureOf(types, SampleTypedLeafPrimitive.class))
            .isEqualTo(structureOf(types, SampleEmpty.class))
            .hasDeclaredMethods("getFoo", "setFoo", "hasFoo")
            .isPublic()
            .isInterface();

        assertThat(templateOf(types, SampleTypedLeafPrimitive.class))
            .hasOnlyDeclaredFields("foo")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafPrimitive.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleTypedLeafPrimitive.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleTypedLeafPrimitive.class)))
            .isPublic();

        assertThat(projectionOf(types, SampleEmpty.class))
            .hasOnlyDeclaredFields("delegate")
            .hasDeclaredMethods("wrap", "unwrap")
            .satisfies(type -> type.isAssignableFrom(structureOf(types, SampleEmpty.class)))
            .isPublic();

        SampleTypedLeafPrimitive primitive = new SampleTypedLeafPrimitive();
        Object fooProjection = projectionInstanceOf(types, primitive);
        assertThat(has("hasFoo", fooProjection)).isEqualTo(PropertyDefinition.SINGLE);
        assertThat(getOptional("getFoo", fooProjection)).contains(0);
        primitive.setFoo(42);
        assertThat(getOptional("getFoo", fooProjection)).contains(42);
        set("setFoo", fooProjection, 84);
        assertThat(primitive.getFoo()).isEqualTo(84);

        SampleEmpty empty = new SampleEmpty();
        Object emptyProjection = projectionInstanceOf(types, empty);
        assertThat(has("hasFoo", emptyProjection)).isEqualTo(PropertyDefinition.MISSING);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();
        set("setFoo", emptyProjection, 42);
        assertThat(getOptional("getFoo", emptyProjection)).isEmpty();

        Object template = templateInstanceOf(types, SampleTypedLeafPrimitive.class);
        assertThat(has("hasFoo", template)).isEqualTo(PropertyDefinition.OPTIONAL);
        assertThat(getOptional("getFoo", template)).isEmpty();
        set("setFoo", template, 42);
        assertThat(getOptional("getFoo", template)).contains(42);
    }

    @Test
    public void can_use_predefined_structures() {
        List<Class<?>> leaf = compiler.apply(new StructuralType().make(SampleTypedLeafFoo.class));

        Map<ClassName, JavaFile> types = new StructuralType().withPredefinitions(
            leaf.stream().filter(type -> type.isAnnotationPresent(CompoundOf.class)).toArray(Class<?>[]::new)
        ).make(SampleBranchFoo.class);

        assertThat(types.keySet()).doesNotContainAnyElementsOf(leaf.stream().map(ClassName::get).collect(Collectors.toSet()));
    }

    @Test
    public void can_throw_exception_on_undefined_property_setter() {
        List<Class<?>> types = compiler.apply(new StructuralType().withExceptionOnEmptySetter(true).make(
            SampleEmpty.class,
            SampleTypedLeafFoo.class
        ));

        SampleEmpty empty = new SampleEmpty();
        Object projection = projectionInstanceOf(types, empty);

        assertThatThrownBy(() -> set("setFoo", projection, "value"))
            .hasRootCauseInstanceOf(UnsupportedOperationException.class)
            .hasRootCauseMessage("foo");
    }

    private static Class<?> projectionOf(List<Class<?>> candidates, Class<?> type) {
        return candidates.stream()
            .filter(candidate -> candidate.isAnnotationPresent(ProjectionOf.class))
            .filter(candidate -> candidate.getAnnotation(ProjectionOf.class).value() == type)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cannot find projection for " + type));
    }

    private static Class<?> structureOf(List<Class<?>> candidates, Class<?> type) {
        return projectionOf(candidates, type).getAnnotation(DelegationOf.class).value();
    }

    private static Class<?> templateOf(List<Class<?>> candidates, Class<?> type) {
        return structureOf(candidates, type).getAnnotation(TemplatedBy.class).value();
    }

    private static Class<?> expansionOf(List<Class<?>> candidates, Class<?> type, Class<?> expansion) {
        return Arrays.stream(structureOf(candidates, type).getAnnotation(DelegatedBy.class).value())
            .filter(candidate -> candidate.isAnnotationPresent(ExpansionOf.class))
            .filter(candidate -> candidate.getAnnotation(ProjectionOf.class).value() == expansion)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cannot resolve expansion " + expansion + " of " + type));
    }

    private static Class<?> enumerationOf(List<Class<?>> candidates, Class<?> type) {
        return candidates.stream()
            .filter(candidate -> candidate.isAnnotationPresent(EnumerationOf.class))
            .filter(candidate -> Arrays.asList(candidate.getAnnotation(EnumerationOf.class).value()).contains(type))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Cannot find enumeration for " + type));
    }

    private static Object projectionInstanceOf(List<Class<?>> candidates, Object instance) {
        try {
            return projectionOf(candidates, instance.getClass())
                .getMethod("wrap", instance.getClass())
                .invoke(null, instance);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object templateInstanceOf(List<Class<?>> candidates, Class<?> type) {
        try {
            return templateOf(candidates, type)
                .getConstructor()
                .newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object templateExpansionInstanceOf(List<Class<?>> candidates, Class<?> type, Object value) {
        try {
            return templateOf(candidates, type)
                .getConstructor(value.getClass())
                .newInstance(value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object expansionInstanceOf(
        List<Class<?>> candidates, Class<?> type, Class<?> expansion, Object value
    ) {
        try {
            return expansionOf(candidates, type, expansion)
                .getMethod("wrap", value.getClass())
                .invoke(null, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object enumerationConstantOf(
        List<Class<?>> candidates, Class<?> type, String name
    ) {
        return Enum.valueOf((Class) enumerationOf(candidates, type), name);
    }

    private static Object get(String getter, Object instance) {
        try {
            return instance.getClass().getMethod(getter).invoke(instance);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getOptional(String getter, Object instance) {
        Object value = get(getter, instance);
        if (value instanceof Optional<?>) {
            return (Optional<T>) value;
        } else {
            throw new AssertionError(getter);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(String getter, Object instance) {
        Object value = get(getter, instance);
        if (value instanceof List<?>) {
            return (List<T>) value;
        } else {
            throw new AssertionError(getter);
        }
    }

    private static void set(String setter, Object instance, Object value) {
        try {
            Arrays.stream(instance.getClass().getMethods())
                .filter(method -> method.getName().equals(setter)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isInstance(value))
                .findFirst()
                .orElseThrow(AssertionError::new)
                .invoke(instance, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static PropertyDefinition has(String has, Object instance) {
        try {
            return (PropertyDefinition) instance.getClass().getMethod(has).invoke(instance);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
