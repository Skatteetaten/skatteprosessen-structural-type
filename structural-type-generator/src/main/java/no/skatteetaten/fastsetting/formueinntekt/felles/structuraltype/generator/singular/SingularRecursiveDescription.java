package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.singular;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

class SingularRecursiveDescription implements SingularDescription {

    private final Supplier<SingularDescription> reference;

    SingularRecursiveDescription(Supplier<SingularDescription> reference) {
        this.reference = reference;
    }

    @Override
    public Class<?> getType() {
        return reference.get().getType();
    }

    @Override
    public boolean isLeaf() {
        return reference.get().isLeaf();
    }

    @Override
    public Map<String, Property> getProperties() {
        return reference.get().getProperties();
    }

    @Override
    public Optional<SingularDescription> getSuperDescription() {
        return reference.get().getSuperDescription();
    }

    @Override
    public List<SingularDescription> getSubDescriptions() {
        return reference.get().getSubDescriptions();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object instanceof SingularDescription) {
            return reference.get().equals(object);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return reference.get().hashCode();
    }

    @Override
    public String toString() {
        return reference.get().toString();
    }
}
