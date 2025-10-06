package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.lang.annotation.Annotation;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

public class SimpleTypeResolver implements TypeResolver.WithPairedMerge {

    private static final Set<Class<?>> PRIMITIVES = Set.of(
        boolean.class,
        byte.class,
        short.class,
        char.class,
        int.class,
        long.class,
        float.class,
        double.class,
        Boolean.class,
        Byte.class,
        Short.class,
        Character.class,
        Integer.class,
        Long.class,
        Float.class,
        Double.class
    );

    private final EnumHandler enumHandler;

    public SimpleTypeResolver() {
        enumHandler = new EnumHandler.Simple();
    }

    public SimpleTypeResolver(EnumHandler enumHandler) {
        this.enumHandler = enumHandler;
    }

    @Override
    public Class<?> merge(Class<?> current, Class<?> next) {
        if (current == next) {
            return current;
        } else if ((current.isEnum() || current == String.class) && (next.isEnum() || next == String.class)) {
            return String.class;
        } else if (current.isEnum() && PRIMITIVES.contains(next) || PRIMITIVES.contains(current) && next.isEnum()) {
            return String.class;
        } else {
            return Generalization.of(current).findMostSpecificWith(Generalization.of(next));
        }
    }

    @Override
    public Optional<CodeBlock> convert(Class<?> source, Class<?> target, CodeBlock value) {
        if (source.equals(target)) {
            return Optional.empty();
        } else if (source == String.class && target.isEnum()) {
            return Optional.of(CodeBlock.builder().add(
                "$L == null ? null : $T.$L($L)",
                value, target, enumHandler.deserializer(target), value
            ).build());
        } else if (source.isEnum() && target == String.class) {
            return Optional.of(CodeBlock.builder().add(
                "$L == null ? null : $L.$L()",
                value, value, enumHandler.serializer(source)
            ).build());
        } else {
            return Optional.of(
                Generalization.convert(source, target).apply(value).build()
            ).map(code -> source.isPrimitive() ? code : CodeBlock.builder().add(
                "$L == null ? $L : $L",
                value, target == boolean.class ? "false" : target.isPrimitive() ? "0" : "null", code
            ).build());
        }
    }

    private enum Generalization {

        PRIMITIVE_BOOLEAN(boolean.class, new Definition(
            Boolean.class,
            code -> CodeBlock.builder().add("$L", code),
            code -> CodeBlock.builder().add("$L", code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$T.valueOf($L)", String.class, code),
            code -> CodeBlock.builder().add("$T.parseBoolean($L)", Boolean.class, code)
        )),

        PRIMITIVE_BYTE(byte.class, Definition.ofNumericPrimitiveType(Byte.class, "byteValue", "parseByte")),

        PRIMITIVE_SHORT(short.class, Definition.ofNumericPrimitiveType(Short.class, "shortValue", "parseShort")),

        PRIMITIVE_CHARACTER(char.class, new Definition(
            Character.class,
            code -> CodeBlock.builder().add("$T.valueOf($L)", Character.class, code),
            code -> CodeBlock.builder().add("$L.charValue()", code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$T.valueOf($L)", String.class, code),
            code -> CodeBlock.builder().add("$T.valueOf($L.charAt(0))", Character.class, code)
        )),

        PRIMITIVE_INTEGER(int.class, Definition.ofNumericPrimitiveType(Integer.class, "intValue", "parseInt")),

        PRIMITIVE_LONG(long.class, Definition.ofNumericPrimitiveType(Long.class, "longValue", "parseLong")),

        PRIMITIVE_FLOAT(float.class, Definition.ofNumericPrimitiveType(Float.class, "floatValue", "parseFloat")),

        PRIMITIVE_DOUBLE(double.class, Definition.ofNumericPrimitiveType(Double.class, "doubleValue", "parseDouble")),

        BOOLEAN(Boolean.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.valueOf($L)", Boolean.class, code)
        )),

        BYTE(Byte.class, Definition.ofNumericWrapperType(Byte.class, "byteValue")),

        SHORT(Short.class, Definition.ofNumericWrapperType(Short.class, "shortValue")),

        CHARACTER(Character.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$T.valueOf($L)", String.class, code),
            code -> CodeBlock.builder().add("$T.valueOf($L.charAt(0))", Character.class, code)
        )),

        INTEGER(Integer.class, Definition.ofNumericWrapperType(Integer.class, "intValue")),

        LONG(Long.class, Definition.ofNumericWrapperType(Long.class, "longValue")),

        FLOAT(Float.class, Definition.ofNumericWrapperType(Float.class, "floatValue")),

        DOUBLE(Double.class, Definition.ofNumericWrapperType(Double.class, "doubleValue")),

        BIG_INTEGER(BigInteger.class, new Definition(
            BigDecimal.class,
            code -> CodeBlock.builder().add("new $T($L)", BigDecimal.class, code),
            code -> CodeBlock.builder().add("$L.toBigInteger()", code)
        ), new Definition(
            Number.class,
            code -> CodeBlock.builder().add("($T) $L", Number.class, code),
            code -> CodeBlock.builder().add("new $T($L.toString())", BigInteger.class, code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("new $T($L)", BigInteger.class, code)
        )),

        BIG_DECIMAL(BigDecimal.class, new Definition(
            Number.class,
            code -> CodeBlock.builder().add("($T) $L", Number.class, code),
            code -> CodeBlock.builder().add("new $T($L.toString())", BigDecimal.class, code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("new $T($L)", BigDecimal.class, code)
        )),

        LOCAL_DATE(LocalDate.class, new Definition(
            LocalDateTime.class,
            code -> CodeBlock.builder().add("$L.atStartOfDay()", code),
            code -> CodeBlock.builder().add("$L.toLocalDate()", code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", LocalDate.class, code)
        )),

        LOCAL_TIME(LocalTime.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", LocalTime.class, code)
        )),

        LOCAL_DATE_TIME(LocalDateTime.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", LocalDateTime.class, code)
        )),

        OFFSET_DATE_TIME(OffsetDateTime.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", OffsetDateTime.class, code)
        )),

        OFFSET_TIME(OffsetTime.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", OffsetTime.class, code)
        )),

        ZONED_DATE_TIME(ZonedDateTime.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.parse($L)", ZonedDateTime.class, code)
        )),

        YEAR(Year.class, new Definition(
            int.class,
            code -> CodeBlock.builder().add("$L.getValue()", code),
            code -> CodeBlock.builder().add("$T.of($L)", Year.class, code)
        ), new Definition(
            Integer.class,
            code -> CodeBlock.builder().add("($T) $L.getValue()", Integer.class, code),
            code -> CodeBlock.builder().add("$T.of($L.intValue())", Year.class, code)
        ), new Definition(
            BigInteger.class,
            code -> CodeBlock.builder().add("$T.valueOf($L.getValue())", BigInteger.class, code),
            code -> CodeBlock.builder().add("$T.of($L.intValue())", Year.class, code)
        ), new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add("$T.of($T.parseInt($L))", Year.class, Integer.class, code)
        )),

        NUMBER(Number.class, new Definition(
            String.class,
            code -> CodeBlock.builder().add("$L.toString()", code),
            code -> CodeBlock.builder().add(
                "$L.indexOf('.') == -1 ? ($T) $T.valueOf($L) : ($T) $T.valueOf($L)",
                code, Number.class, Long.class, code, Number.class, Double.class, code
            )
        )),

        STRING(String.class);

        private static final Map<TypeName, SimpleTypeResolver.Generalization> TYPES;
        private static final Map<Conversion, Function<CodeBlock, CodeBlock.Builder>> CONVERSIONS;

        static {
            TYPES = Arrays.stream(values()).collect(Collectors.toMap(
                generalization -> TypeName.get(generalization.type),
                Function.identity()
            ));
            CONVERSIONS = Arrays.stream(values()).flatMap(generalization -> generalization.definitions.stream().flatMap(
                definition -> Stream.of(Map.entry(
                    new Conversion(generalization.type, definition.target), definition.encoder
                ), Map.entry(
                    new Conversion(definition.target, generalization.type), definition.decoder
                ))
            )).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        private final Class<?> type;
        private final List<Definition> definitions;

        private final Set<Class<?>> representations;

        Generalization(Class<?> type, Definition... definitions) {
            this(type, Arrays.asList(definitions));
        }

        Generalization(Class<?> type, List<Definition> definitions) {
            this.type = type;
            this.definitions = definitions;
            representations = Stream.concat(
                Stream.of(type),
                definitions.stream().map(definition -> definition.target)
            ).collect(Collectors.toSet());
        }

        static Generalization of(Class<?> type) {
            return of(TypeName.get(type));
        }

        static Generalization of(TypeName name) {
            Generalization generalization = TYPES.get(name);
            if (generalization == null) {
                throw new IllegalArgumentException("Do not know how to generalize type: " + name);
            }
            return generalization;
        }

        static Function<CodeBlock, CodeBlock.Builder> convert(Class<?> source, Class<?> target) {
            Function<CodeBlock, CodeBlock.Builder> converter = CONVERSIONS.get(new Conversion(source, target));
            if (converter == null) {
                throw new IllegalArgumentException("Unknown conversion from " + source + " to " + target);
            } else {
                return converter;
            }
        }

        Class<?> findMostSpecificWith(SimpleTypeResolver.Generalization other) {
            return representations.stream()
                .filter(other.representations::contains)
                .map(Generalization::of)
                .min(Comparator.comparing(Enum::ordinal))
                .map(generalization -> generalization.type)
                .orElseThrow(() -> new IllegalStateException("Cannot generalize " + type + " and " + other.type));
        }

        private static class Definition {

            private final Class<?> target;

            private final Function<CodeBlock, CodeBlock.Builder> encoder, decoder;

            Definition(
                Class<?> target,
                Function<CodeBlock, CodeBlock.Builder> encoder,
                Function<CodeBlock, CodeBlock.Builder> decoder
            ) {
                this.target = target;
                this.encoder = encoder;
                this.decoder = decoder;
            }

            static List<Definition> ofNumericPrimitiveType(Class<?> boxed, String unwrap, String wrap) {
                return Stream.of(new Definition(
                    boxed,
                    code -> CodeBlock.builder().add("$L", code),
                    code -> CodeBlock.builder().add("$L", code)
                ), new Definition(
                    BigDecimal.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L)", BigDecimal.class, code),
                    code -> CodeBlock.builder().add("$L.$N()", code, unwrap)
                ), new Definition(
                    BigInteger.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L)", BigInteger.class, code),
                    code -> CodeBlock.builder().add("$L.$N()", code, unwrap)
                ), new Definition(
                    Number.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L)", boxed, code),
                    code -> CodeBlock.builder().add("$L.$N()", code, unwrap)
                ), new Definition(
                    String.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L)", String.class, code),
                    code -> CodeBlock.builder().add("$T.$N($L)", boxed, wrap, code)
                )).filter(definition ->
                    boxed != Float.class && boxed != Double.class || definition.target != BigInteger.class
                ).collect(Collectors.toList());
            }

            static List<Definition> ofNumericWrapperType(Class<?> boxed, String unwrap) {
                return Stream.of(new Definition(
                    Number.class,
                    code -> CodeBlock.builder().add("($T) $L", Number.class, code),
                    code -> CodeBlock.builder().add("$T.valueOf($L.$N())", boxed, code, unwrap)
                ), new Definition(
                    BigDecimal.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L.$N())", BigDecimal.class, code, unwrap),
                    code -> CodeBlock.builder().add("$T.valueOf($L.$N())", boxed, code, unwrap)
                ), new Definition(
                    BigInteger.class,
                    code -> CodeBlock.builder().add("$T.valueOf($L.$N())", BigInteger.class, code, unwrap),
                    code -> CodeBlock.builder().add("$T.valueOf($L.$N())", boxed, code, unwrap)
                ), new Definition(
                    String.class,
                    code -> CodeBlock.builder().add("$L.toString()", code),
                    code -> CodeBlock.builder().add("$T.valueOf($L)", boxed, code)
                )).filter(definition ->
                    boxed != Float.class && boxed != Double.class || definition.target != BigInteger.class
                ).collect(Collectors.toList());
            }
        }

        private static class Conversion {

            private final Class<?> source, target;

            Conversion(Class<?> source, Class<?> target) {
                this.source = source;
                this.target = target;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) {
                    return true;
                }
                if (object == null || getClass() != object.getClass()) {
                    return false;
                }
                Conversion conversion = (Conversion) object;
                return source == conversion.source && target == conversion.target;
            }

            @Override
            public int hashCode() {
                int result = source.hashCode();
                result = 31 * result + target.hashCode();
                return result;
            }
        }
    }

    public interface EnumHandler {

        String serializer(Class<?> type);

        String deserializer(Class<?> type);

        class Simple implements EnumHandler {

            @Override
            public String serializer(Class<?> type) {
                return "name";
            }

            @Override
            public String deserializer(Class<?> type) {
                return "valueOf";
            }
        }


        class UsingJaxb implements EnumHandler {

            private final Class<? extends Annotation> xmlEnum;

            @SuppressWarnings("unchecked")
            private UsingJaxb(ClassLoader classLoader, String namespace) {
                try {
                    xmlEnum = (Class<? extends Annotation>) Class.forName(namespace + ".XmlEnum", true, classLoader);
                } catch (Exception e) {
                    throw new IllegalStateException("Could not resolve JAXB for namespace " + namespace + " in " + classLoader, e);
                }
            }

            public static EnumHandler ofJavax() {
                return ofJavax(JaxbFallbackBeanAccessResolver.class.getClassLoader());
            }

            public static EnumHandler ofJavax(ClassLoader classLoader) {
                return new UsingJaxb(classLoader, "javax.xml.bind.annotation");
            }

            public static EnumHandler ofJakarta() {
                return ofJakarta(JaxbFallbackBeanAccessResolver.class.getClassLoader());
            }

            public static EnumHandler ofJakarta(ClassLoader classLoader) {
                return new UsingJaxb(classLoader, "jakarta.xml.bind.annotation");
            }

            @Override
            public String serializer(Class<?> type) {
                return type.isAnnotationPresent(xmlEnum) ? "value" : "name";
            }

            @Override
            public String deserializer(Class<?> type) {
                return type.isAnnotationPresent(xmlEnum) ? "fromValue" : "valueOf";
            }
        }
    }
}
