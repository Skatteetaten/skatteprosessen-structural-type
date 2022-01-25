package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.util.Map;

class SingularBranchDescription implements SingularDescription {

    private final Class<?> type;
    private final Map<String, Property> properties;

    SingularBranchDescription(Class<?> type, Map<String, Property> properties) {
        this.type = type;
        this.properties = properties;
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
