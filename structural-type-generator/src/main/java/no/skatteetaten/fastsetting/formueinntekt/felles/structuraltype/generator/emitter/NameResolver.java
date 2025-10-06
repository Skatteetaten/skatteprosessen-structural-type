package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.emitter;

import java.util.List;
import java.util.stream.Collectors;

import com.squareup.javapoet.ClassName;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.compound.CompoundDescription;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular.SingularDescription;

public interface NameResolver {

    ClassName structure(CompoundDescription compound);

    ClassName template(CompoundDescription compound);

    ClassName projection(CompoundDescription compound, SingularDescription singular);

    default List<ClassName> projections(CompoundDescription compound) {
        return compound.getSingulars().stream()
            .map(singular -> projection(compound, singular))
            .collect(Collectors.toList());
    }
}
