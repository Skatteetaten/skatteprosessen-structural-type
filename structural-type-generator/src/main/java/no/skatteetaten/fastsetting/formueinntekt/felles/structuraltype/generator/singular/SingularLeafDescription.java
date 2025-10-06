package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SingularLeafDescription implements SingularDescription {

    private final Class<?> type;

    SingularLeafDescription(Class<?> type) {
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Map<String, Property> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Optional<SingularDescription> getSuperDescription() {
        return Optional.empty();
    }

    @Override
    public List<SingularDescription> getSubDescriptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof SingularDescription) {
            SingularDescription description = (SingularDescription) object;
            return description.isLeaf() && description.getType() == type;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 31 + type.hashCode();
    }

    @Override
    public String toString() {
        return "Singular{sort=leaf,type=" + type.getTypeName() + "}";
    }
}
