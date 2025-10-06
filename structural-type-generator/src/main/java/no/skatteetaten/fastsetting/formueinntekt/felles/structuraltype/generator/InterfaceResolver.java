package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.List;

import com.squareup.javapoet.ClassName;

@FunctionalInterface
public interface InterfaceResolver {

    List<Class<?>> resolve(ClassName structure, List<Class<?>> components);
}
