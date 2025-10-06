package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.util.List;
import java.util.Optional;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

public enum Cardinality {

    OPTIONAL {
        @Override
        public TypeName asReturnType(TypeName type) {
            return ParameterizedTypeName.get(ClassName.get(Optional.class), type);
        }
    },

    SINGLE,

    LIST {
        @Override
        public TypeName asPropertyType(TypeName type) {
            return ParameterizedTypeName.get(ClassName.get(List.class), type);
        }
    };

    public Cardinality merge(Cardinality cardinality) {
        switch (cardinality) {
        case LIST:
            return LIST;
        case SINGLE:
            return this;
        case OPTIONAL:
            return this == LIST ? LIST : OPTIONAL;
        default:
            throw new IllegalStateException();
        }
    }

    public TypeName asPropertyType(TypeName type) {
        return type;
    }

    public TypeName asReturnType(TypeName type) {
        return asPropertyType(type);
    }
}
