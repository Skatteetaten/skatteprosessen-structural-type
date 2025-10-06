package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumeratedAs {

    String value();
}
