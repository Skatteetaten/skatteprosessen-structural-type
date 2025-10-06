package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.*;

@CompoundOf(SimpleStructure.Delegate.class)
@DelegatedBy(SimpleStructure.Projection.class)
@TemplatedBy(SimpleStructure.Template.class)
public interface SimpleStructure {

    String getValue();

    void setValue(String value);

    @TemplateOf(SimpleStructure.class)
    class Template implements SimpleStructure {

        private String value;

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }
    }

    @ProjectionOf(Delegate.class)
    @DelegationOf(SimpleStructure.class)
    class Projection implements SimpleStructure {

        private Delegate delegate = new Delegate();

        @Override
        public String getValue() {
            return delegate.getValue();
        }

        @Override
        public void setValue(String value) {
            delegate.setValue(value);
        }
    }

    class Delegate {

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
