package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.sample;

public class SampleEnumNestedLeafFoo {

    private SampleEnumLeafFoo foo;

    private SampleEnumFoo bar;

    public SampleEnumLeafFoo getFoo() {
        return foo;
    }

    public void setFoo(SampleEnumLeafFoo foo) {
        this.foo = foo;
    }

    public SampleEnumFoo getBar() {
        return bar;
    }

    public void setBar(SampleEnumFoo bar) {
        this.bar = bar;
    }
}
