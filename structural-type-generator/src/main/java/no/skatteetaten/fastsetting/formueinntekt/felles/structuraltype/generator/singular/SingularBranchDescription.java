package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class SingularBranchDescription implements SingularDescription {

    private final Class<?> type;
    private final Map<String, Property> properties;
    private final SingularDescription superType;
    private final List<SingularDescription> subTypes;

    SingularBranchDescription(
        Class<?> type,
        Map<String, Property> properties,
        SingularDescription superDescription,
        List<SingularDescription> subDescriptions
    ) {
        this.type = type;
        this.properties = properties;
        this.superType = superDescription;
        this.subTypes = subDescriptions;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Map<String, Property> getProperties() {
        return properties;
    }

    @Override
    public Optional<SingularDescription> getSuperDescription() {
        return Optional.ofNullable(superType);
    }

    @Override
    public List<SingularDescription> getSubDescriptions() {
        return subTypes;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof SingularDescription) {
            SingularDescription description = (SingularDescription) object;
            return !description.isLeaf() && description.getType() == type;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 17 + type.hashCode();
    }

    @Override
    public String toString() {
        return "Singular{sort=branch,type=" + type.getTypeName() + "}";
    }
}
