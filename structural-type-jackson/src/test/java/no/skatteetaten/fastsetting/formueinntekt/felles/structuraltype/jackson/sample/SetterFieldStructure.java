package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.jackson.sample;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.*;

@CompoundOf(SetterFieldStructure.Delegate.class)
@DelegatedBy(SetterFieldStructure.Projection.class)
public interface SetterFieldStructure {

    String getValue();

    void setValue(String value);

    @CompoundOf(OuterDelegate.class)
    @DelegatedBy(OuterProjection.class)
    interface OuterStructure {

        SetterFieldStructure getInner();

        void setInner(SetterFieldStructure inner);
    }

    @ProjectionOf(SimpleStructure.Delegate.class)
    @DelegationOf(SetterFieldStructure.class)
    class Projection implements SetterFieldStructure {

        private final Delegate delegate;

        public Projection(Delegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getValue() {
            return delegate.getValue();
        }

        @Override
        public void setValue(String value) {
            delegate.setValue(value);
        }
    }

    @ProjectionOf(OuterDelegate.class)
    @DelegationOf(OuterStructure.class)
    class OuterProjection implements OuterStructure {

        private final OuterDelegate delegate = new OuterDelegate();

        @Override
        public SetterFieldStructure getInner() {
            return new Projection(delegate.getDelegate());
        }

        @Override
        public void setInner(SetterFieldStructure value) {
            delegate.setDelegate(((Projection) value).delegate);
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

    class OuterDelegate {

        private Delegate delegate = new Delegate();

        public Delegate getDelegate() {
            return delegate;
        }

        public void setDelegate(Delegate delegate) {
            this.delegate = delegate;
        }
    }

    class Wrapper {

        private OuterStructure outer = new OuterProjection();

        public OuterStructure getOuter() {
            return outer;
        }

        public void setOuter(OuterStructure outer) {
            this.outer = outer;
        }
    }
}
