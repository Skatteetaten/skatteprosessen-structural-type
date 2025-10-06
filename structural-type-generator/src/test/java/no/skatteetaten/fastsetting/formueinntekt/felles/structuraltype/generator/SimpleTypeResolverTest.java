package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

@RunWith(Parameterized.class)
public class SimpleTypeResolverTest {

    @Parameterized.Parameters(name = "{0} + {1} -> {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // boolean
            { boolean.class, boolean.class, boolean.class, true, true, false, false },
            { boolean.class, Boolean.class, Boolean.class, true, true, false, false },
            { boolean.class, String.class, String.class, true, "true", "foo", "foo" },
            // byte
            { byte.class, byte.class, byte.class, (byte) 42, (byte) 42, (byte) 84, (byte) 84 },
            { byte.class, Byte.class, Byte.class, (byte) 42, (byte) 42, (byte) 84, (byte) 84 },
            { byte.class, BigDecimal.class, BigDecimal.class, (byte) 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { byte.class, BigInteger.class, BigInteger.class, (byte) 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { byte.class, Number.class, Number.class, (byte) 42, (byte) 42, 84, 84 },
            { byte.class, String.class, String.class, (byte) 42, "42", "foo", "foo" },
            // short
            { short.class, short.class, short.class, (short) 42, (short) 42, (short) 84, (short) 84 },
            { short.class, Short.class, Short.class, (short) 42, (short) 42, (short) 84, (short) 84 },
            { short.class, BigDecimal.class, BigDecimal.class, (short) 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { short.class, BigInteger.class, BigInteger.class, (short) 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { short.class, Number.class, Number.class, (short) 42, (short) 42, 84, 84 },
            { short.class, String.class, String.class, (short) 42, "42", "foo", "foo" },
            // char
            { char.class, char.class, char.class, (char) 42, (char) 42, (char) 84, (char) 84 },
            { char.class, Character.class, Character.class, (char) 42, (char) 42, (char) 84, (char) 84 },
            { char.class, String.class, String.class, (char) 42, String.valueOf((char) 42), "foo", "foo" },
            // int
            { int.class, int.class, int.class, 42, 42, 84, 84 },
            { int.class, Integer.class, Integer.class, 42, 42, 84, 84 },
            { int.class, BigDecimal.class, BigDecimal.class, 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { int.class, BigInteger.class, BigInteger.class, 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { int.class, Number.class, Number.class, 42, 42, 84, 84 },
            { int.class, String.class, String.class, 42, "42", "foo", "foo" },
            // long
            { long.class, long.class, long.class, 42L, 42L, 84L, 84L },
            { long.class, Long.class, Long.class, 42L, 42L, 84L, 84L },
            { long.class, BigDecimal.class, BigDecimal.class, 42L, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { long.class, BigInteger.class, BigInteger.class, 42L, BigInteger.valueOf(42L), BigInteger.TEN, BigInteger.TEN },
            { long.class, Number.class, Number.class, 42L, 42L, 84, 84 },
            { long.class, String.class, String.class, 42L, "42", "foo", "foo" },
            // float
            { float.class, float.class, float.class, 42f, 42f, 84f, 84f },
            { float.class, Float.class, Float.class, 42f, 42f, 84f, 84f },
            { float.class, BigDecimal.class, BigDecimal.class, 42f, BigDecimal.valueOf(42f), BigDecimal.TEN, BigDecimal.TEN },
            { float.class, Number.class, Number.class, 42f, 42f, 84, 84 },
            { float.class, String.class, String.class, 42f, "42.0", "foo", "foo" },
            // double
            { double.class, double.class, double.class, 42d, 42d, 84d, 84d },
            { double.class, Double.class, Double.class, 42d, 42d, 84d, 84d },
            { double.class, BigDecimal.class, BigDecimal.class, 42d, BigDecimal.valueOf(42d), BigDecimal.TEN, BigDecimal.TEN },
            { double.class, Number.class, Number.class, 42d, 42d, 84, 84 },
            { double.class, String.class, String.class, 42d, "42.0", "foo", "foo" },
            // java.lang.Boolean
            { Boolean.class, Boolean.class, Boolean.class, true, true, false, false },
            { Boolean.class, String.class, String.class, true, "true", "foo", "foo" },
            // java.lang.Byte
            { Byte.class, Byte.class, Byte.class, (byte) 42, (byte) 42, (byte) 84, (byte) 84 },
            { Byte.class, BigInteger.class, BigInteger.class, (byte) 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { Byte.class, BigDecimal.class, BigDecimal.class, (byte) 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { Byte.class, Number.class, Number.class, (byte) 42, (byte) 42, 84, 84 },
            { Byte.class, String.class, String.class, (byte) 42, "42", "foo", "foo" },
            // java.lang.Short
            { Short.class, Short.class, Short.class, (short) 42, (short) 42, (short) 84, (short) 84 },
            { Short.class, BigInteger.class, BigInteger.class, (short) 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { Short.class, BigDecimal.class, BigDecimal.class, (short) 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { Short.class, Number.class, Number.class, (short) 42, (short) 42, 84, 84 },
            { Short.class, String.class, String.class, (short) 42, "42", "foo", "foo" },
            // java.lang.Character
            { Character.class, Character.class, Character.class, (char) 42, (char) 42, (char) 84, (char) 84 },
            { Character.class, String.class, String.class, (char) 42, String.valueOf((char) 42), "foo", "foo" },
            // java.lang.Integer
            { Integer.class, Integer.class, Integer.class, 42, 42, 84, 84 },
            { Integer.class, BigInteger.class, BigInteger.class, 42, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { Integer.class, BigDecimal.class, BigDecimal.class, 42, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { Integer.class, Number.class, Number.class, 42, 42, 84, 84 },
            { Integer.class, String.class, String.class, 42, "42", "foo", "foo" },
            // java.lang.Long
            { Long.class, Long.class, Long.class, 42L, 42L, 84L, 84L },
            { Long.class, BigInteger.class, BigInteger.class, 42L, BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { Long.class, BigDecimal.class, BigDecimal.class, 42L, BigDecimal.valueOf(42), BigDecimal.TEN, BigDecimal.TEN },
            { Long.class, Number.class, Number.class, 42L, 42L, 84, 84 },
            { Long.class, String.class, String.class, 42L, "42", "foo", "foo" },
            // java.lang.Float
            { Float.class, Float.class, Float.class, 42f, 42f, 84f, 84f },
            { Float.class, BigDecimal.class, BigDecimal.class, 42f, BigDecimal.valueOf(42f), BigDecimal.TEN, BigDecimal.TEN },
            { Float.class, Number.class, Number.class, 42f, 42f, 84, 84 },
            { Float.class, String.class, String.class, 42f, "42.0", "foo", "foo" },
            // java.lang.Double
            { Double.class, Double.class, Double.class, 42d, 42d, 84d, 84d },
            { Double.class, BigDecimal.class, BigDecimal.class, 42d, BigDecimal.valueOf(42d), BigDecimal.TEN, BigDecimal.TEN },
            { Double.class, Number.class, Number.class, 42d, 42d, 84, 84 },
            { Double.class, String.class, String.class, 42d, "42.0", "foo", "foo" },
            // java.math.BigInteger
            { BigInteger.class, BigInteger.class, BigInteger.class, BigInteger.ONE, BigInteger.ONE, BigInteger.TEN, BigInteger.TEN },
            { BigInteger.class, Number.class, Number.class, BigInteger.ONE, BigInteger.ONE, 84, 84 },
            { BigInteger.class, String.class, String.class, BigInteger.ONE, "1", "foo", "foo" },
            // java.math.BigDecimal
            { BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN },
            { BigDecimal.class, Number.class, Number.class, BigDecimal.ONE, BigDecimal.ONE, 84, 84 },
            { BigDecimal.class, String.class, String.class, BigDecimal.ONE, "1", "foo", "foo" },
            // java.time.LocalTime
            { LocalTime.class, LocalTime.class, LocalTime.class, LocalTime.MIN, LocalTime.MIN, LocalTime.MAX, LocalTime.MAX },
            { LocalTime.class, String.class, String.class, LocalTime.MIN, LocalTime.MIN.toString(), "foo", "foo" },
            // java.time.LocalDate
            { LocalDate.class, LocalDate.class, LocalDate.class, LocalDate.MIN, LocalDate.MIN, LocalDate.MAX, LocalDate.MAX },
            { LocalDate.class, LocalDateTime.class, LocalDateTime.class, LocalDate.MIN, LocalDate.MIN.atStartOfDay(), LocalDateTime.MIN, LocalDateTime.MIN },
            { LocalDate.class, String.class, String.class, LocalDate.MIN, LocalDate.MIN.toString(), "foo", "foo" },
            // java.time.LocalDateTime
            { LocalDateTime.class, LocalDateTime.class, LocalDateTime.class, LocalDateTime.MIN, LocalDateTime.MIN, LocalDateTime.MAX, LocalDateTime.MAX },
            { LocalDateTime.class, String.class, String.class, LocalDateTime.MIN, LocalDateTime.MIN.toString(), "foo", "foo" },
            // java.time.OffsetTime
            { OffsetTime.class, OffsetTime.class, OffsetTime.class, OffsetTime.MIN, OffsetTime.MIN, OffsetTime.MAX, OffsetTime.MAX },
            { OffsetTime.class, String.class, String.class, OffsetTime.MIN, OffsetTime.MIN.toString(), "foo", "foo" },
            // java.time.OffsetDateTime
            { OffsetDateTime.class, OffsetDateTime.class, OffsetDateTime.class, OffsetDateTime.MIN, OffsetDateTime.MIN, OffsetDateTime.MAX, OffsetDateTime.MAX },
            { OffsetDateTime.class, String.class, String.class, OffsetDateTime.MIN, OffsetDateTime.MIN.toString(), "foo", "foo" },
            // java.time.ZonedDateTime
            { ZonedDateTime.class, ZonedDateTime.class, ZonedDateTime.class, OffsetDateTime.MIN.toZonedDateTime(), OffsetDateTime.MIN.toZonedDateTime(), OffsetDateTime.MAX.toZonedDateTime(), OffsetDateTime.MAX.toZonedDateTime() },
            { ZonedDateTime.class, String.class, String.class, OffsetDateTime.MIN.toZonedDateTime(), OffsetDateTime.MIN.toZonedDateTime().toString(), "foo", "foo" },
            // java.time.Year
            { Year.class, Year.class, Year.class, Year.of(42), Year.of(42), Year.of(84), Year.of(84) },
            { Year.class, int.class, int.class, Year.of(42), 42, 84, 84 },
            { Year.class, Integer.class, Integer.class, Year.of(42), 42, 84, 84 },
            { Year.class, BigInteger.class, BigInteger.class, Year.of(42), BigInteger.valueOf(42), BigInteger.TEN, BigInteger.TEN },
            { Year.class, String.class, String.class, Year.of(42), "42", "foo", "foo" },
            // java.lang.Number
            { Number.class, Number.class, Number.class, 42, 42, 84, 84 },
            { Number.class, String.class, String.class, 42L, "42", "foo", "foo" },
            { Number.class, String.class, String.class, 42d, "42.0", "foo", "foo" },
            // java.lang.String
            { String.class, String.class, String.class, "foo", "foo", "bar", "bar" },
            // java.lang.Enum
            { Enum1.class, String.class, String.class, Enum1.FOO, "FOO", "bar", "bar" },
            { Enum1.class, Enum2.class, String.class, Enum1.FOO, "FOO", Enum2.BAR, "BAR" },
            { Enum1.class, boolean.class, String.class, Enum1.FOO, "FOO", true, "true" },
            { Enum1.class, Boolean.class, String.class, Enum1.FOO, "FOO", Boolean.TRUE, "true" }
        });
    }

    private final Class<?> source, target, merged;
    private final Object sourceValue, mergedSourceValue, targetValue, mergedTargetValue;

    public SimpleTypeResolverTest(
        Class<?> source, Class<?> target, Class<?> merged,
        Object sourceValue, Object mergedSourceValue, Object targetValue, Object mergedTargetValue
    ) {
        this.source = source;
        this.target = target;
        this.merged = merged;
        this.sourceValue = sourceValue;
        this.targetValue = targetValue;
        this.mergedSourceValue = mergedSourceValue;
        this.mergedTargetValue = mergedTargetValue;
    }

    private Function<Map<ClassName, JavaFile>, List<Class<?>>> compiler;

    private TypeResolver.WithPairedMerge typeResolver;

    @Before
    public void setUp() {
        compiler = new InMemoryCompiler();
        typeResolver = new SimpleTypeResolver();
    }

    @Test
    public void testGeneralization() {
        assertThat(typeResolver.merge(source, target)).isEqualTo(merged);
    }

    @Test
    public void testSourceToMergedMapping() {
        assertThat(converter(source, merged, typeResolver.convert(
            source, merged, CodeBlock.builder().add("value").build()
        ).orElseGet(() -> CodeBlock.builder().add("value").build())).apply(sourceValue)).isEqualTo(mergedSourceValue);
    }

    @Test
    public void testTargetToMergedMapping() {
        assertThat(converter(target, merged, typeResolver.convert(
            target, merged, CodeBlock.builder().add("value").build()
        ).orElseGet(() -> CodeBlock.builder().add("value").build())).apply(targetValue)).isEqualTo(mergedTargetValue);
    }

    @Test
    public void testSourceFromMergedMapping() {
        assertThat(converter(merged, source, typeResolver.convert(
            merged, source, CodeBlock.builder().add("value").build()
        ).orElseGet(() -> CodeBlock.builder().add("value").build())).apply(mergedSourceValue)).isEqualTo(sourceValue);
    }

    @Test
    public void testTargetFromMergedMapping() {
        assertThat(converter(merged, target, typeResolver.convert(
            merged, target, CodeBlock.builder().add("value").build()
        ).orElseGet(() -> CodeBlock.builder().add("value").build())).apply(mergedTargetValue)).isEqualTo(targetValue);
    }

    private Function<Object, Object> converter(Class<?> input, Class<?> output, CodeBlock code) {
        List<Class<?>> types = compiler.apply(Collections.singletonMap(
            ClassName.get(SimpleTypeResolverTest.class.getPackageName(), "Sample"),
            JavaFile.builder(
                SimpleTypeResolverTest.class.getPackageName(),
                TypeSpec.classBuilder(ClassName.get(SimpleTypeResolverTest.class.getPackageName(), "Sample"))
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(MethodSpec.methodBuilder("convert")
                        .returns(output)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(input, "value")
                        .addCode(CodeBlock.builder().addStatement("return $L", code).build())
                        .build()).build()
            ).skipJavaLangImports(true).build())
        );

        assertThat(types).hasSize(1);

        return value -> {
            try {
                return types.get(0).getMethod("convert", input).invoke(null, value);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        };
    }

    public enum Enum1 {
        FOO
    }

    public enum Enum2 {
        BAR
    }

    public enum Enum3 {
        FOO
    }
}
