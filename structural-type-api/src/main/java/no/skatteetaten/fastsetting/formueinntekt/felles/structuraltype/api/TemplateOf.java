package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TemplateOf {

    Class<?> value();
}
