Structural types for Java
=========================

This project aims to create a structural representation of similar nominal Java classes. A structural type is represented by an interface that contains property accessors for all properties that are defined by those classes. For each class, a projection class is generated that implements that interface for delegation to the nominal classes for which the structure was created. Additionally, a template implementation is created that implements the structural interface as a Java bean.

Simple example
--------------

Assume that the following classes define similar properties

```java
class Foo {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

class Bar {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}
```

Then the generated structural type, its projections and template would look similar to the following:

```java
interface Structure {
  String getValue();
  void setValue(String value);
}

class FooProjection implements Structure {
  final Foo foo;
  FooProjection(Foo foo) { this.foo = foo; }
  @Override String getValue() { return foo.getValue(); }
  @Override void setValue(String value) { foo.setValue(value); }
}

class BarProjection implements Structure {
  final Bar bar;
  BarProjection(Bar bar) { this.bar = bar; }
  @Override String getValue() { return bar.getValue(); }
  @Override void setValue(String value) { bar.setValue(value); }
}

class Template implements Structure {
  String value;
  @Override String getValue() { return value; }
  @Override void setValue(String value) { this.value = value; }
}
```

Using the structural in your code therefore allows to use an interface abstraction without manual implementation and maintenance of the abstracted type. This helps to avoid implementation errors in classes with many properties and eases maintenance of large groups of similar domain objects that change regularly.

To use the created types, the *structural-type-api* module needs to be added to the class loader using these classes.

Use of the API
--------------

The API is implemented with a single entry point `StructuralType` in the *structrual-type-generator* module:

```java
Map<ClassName, JavaFile> types = new StructuralType().make(Foo.class, Bar.class);
```

The class is immutable and offers different configuration steps to customize its behavior such as:
- `NamingStrategy`: Allows to customize the names of structures, projections and templates. Implementations are `FixedPackageNamingStrategy` where all types are defined within the same package and the `CommonPrefixNamingStrategy` where a common package and type name prefix is computed for the set of input classes (default).
- `PropertyStrategy`: Allows customizing the naming of the methods defined on the structure. The only implementation is `BeanPropertyStrategy` where methods are named in accordance to the Java bean specification.
- `AccessResolver`: Determines how bean properties are accessed by projections. By default, the `BeanAccessResolver` is used which expects accessor methods according to the Java bean specification. Alternatively, `FieldAccessResolver` allows for direct field access if they are visible. An access resolver also determines if a property for a setter
- `StructuralResolver`: Resolves input classes to their property representations. By default, the `SimpleStructuralResolver` resolves all fields as they are defined. Alternatively, the `JaxbStructuralResolver` considers JAXB annotations.
- `TypeResolver`: if two properties are of different types, a type resolver is responsible for resolving those two types to their most general common type and to implement assignments from and to this general from for each projection. By default, the `SimpleTypeResolver` considers primitive types and their wrappers, Java time types, numeric types, enums and strings to be assignable to each other by expanding types to the most general representation. Alternatively, the `StrictTypeResolver` only allows for equal types and throws an exception if this assumption is not met.
- `InterfaceResolver`: Allows for the implementation of additional interfaces for each structural type. By default, no interfaces are added. Included interfaces can only define default methods or parameterless abstract methods that return a non-void value which are also annotated with `@PropertyGetter("property")`. Those methods will be implemented to return the structural property that is the annotation value.
- `NodeResolver`: A node resolver is capable of overriding any property name or enumeration constant value. This property is then transparently used instead of the actual, declared property name.
- `condition`: By registering one or more conditions, properties of types can be fully ignored when creating structural types.
- `exceptionOnEmptySetter`: By default, setters for non-supported properties are non-operational. By enabling this configuration, an exception is thrown instead when setting a property that is not supported.

Mixed property cardinalities
----------------------------

Properties considered by structural types do not necessarily need to implement all properties for all types. If a property is missing on a structural type, the structure will define the getter method to be `Optional` to indicate that a value might be missing on a projected bean. The setter will be non-operational for this property. Additionally, a so-called ownership method is defined that indicates if a value can be defined for a certain instance:

```java
class Foo {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

class Bar { }

interface Structure {
  Optional<String> getValue();
  void setValue(String value);
  PropertyDefinition hasValue();
}

class FooProjection implements Structure {
  final Foo foo;
  FooProjection(Foo foo) { this.foo = foo; }
  @Override Optional<String> getValue() { return Optional.ofNullable(foo.getValue()); }
  @Override void setValue(String value) { foo.setValue(value); }
  @Override PropertyDefinition hasValue() { return PropertyDefinition.SINGLE }
}

class BarProjection implements Structure {
  final Bar bar;
  BarProjection(Bar bar) { this.bar = bar; }
  @Override String getValue() { return Optional.empty(); }
  @Override void setValue(String value) { }
  @Override PropertyDefinition hasValue() { return PropertyDefinition.MISSING }
}

class Template implements Structure {
  String value;
  @Override Optional<String> getValue() { return Optional.ofNullable(value); }
  @Override void setValue(String value) { this.value = value; }
  @Override PropertyDefinition hasValue() { return PropertyDefinition.OPTIONAL }
}
```

A similar projection is implemented for properties that are defined as `List`. If a class `Foo` defined the property as a list whereas `Bar` only defines a single value as a regular field, the structure will promote the structural property type to be a list but map the single list to a singleton list projection. Any combinations of single, list and missing property cardinalities are supported. The template implementation will always assume the most general cardinaltiy.

List properties are not accessible by setters on structures in order to allow projections a direct mapping of list values to projected values. This requires for the projection to control the list implementation. Instead, single-element setters are implemented for all list-typed properties where added elements are appended to the underlying list.

Structures as properties
------------------------

Structural types can themselves define structures as properties. These structures will be resolved transparently:

```java
class Foo {
  Qux value;
  Qux getValue() { return value; }
  void setValue(Qux value) { this.value = value; }
}

class Bar {
  Baz value;
  Baz getValue() { return value; }
  void setValue(Baz value) { this.value = value; }
}

class Qux {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

class Baz {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

interface Structure {
  InnerStructure getValue();
  void setValue(InnerStructure value);
}

interface InnerStructure {
  String getValue();
  void setValue(String value);
}

class FooProjection implements Structure {
  final Foo foo;
  FooProjection(Foo foo) { this.foo = foo; }
  @Override InnerStructure getValue() { return new QuxProjection(foo.getValue()); }
  @Override void setValue(InnerStructure value) { foo.setValue(((QuxProjection) value).qux); }
}

class BarProjection implements Structure {
  final Bar bar;
  BarProjection(Bar bar) { this.bar = bar; }
  @Override InnerStructure getValue() { return new BazProjection(bar.getValue()); }
  @Override void setValue(InnerStructure value) { foo.setValue(((BazProjection) value).baz); }
}

class QuxProjection implements InnerStructure {
  final Qux qux;
  QuxProjection(Foo qux) { this.qux = qux; }
  @Override String getValue() { return foo.getValue(); }
  @Override void setValue(String value) { foo.setValue(value); }
}

class BazProjection implements InnerStructure {
  final Baz baz;
  BazProjection(Baz baz) { this.bar = bar; }
  @Override String getValue() { return baz.getValue(); }
  @Override void setValue(String value) { baz.setValue(value); }
}

class Template implements Structure {
  InnerStructure value;
  @Override InnerStructure getValue() { return value; }
  @Override void setValue(InnerStructure value) { this.value = value; }
}

class InnerTemplate implements InnerStructure {
  String value;
  @Override String getValue() { return value; }
  @Override void setValue(String value) { this.value = value; }
}
```

Null checks and are emitted for reader convenience in both the previous and in all future examples.

As it is obvious from the above code, it is not possible to set a projection of one type as a property of another type as it is not possible to anchor a foreign reference in the delegation target what would break the referential integrity of the setter contract. Instead, additional `merge*` methods are created for each inner structure where a copy of the original value is explicitly set as an inner structure. The copy is then created as a projection of the corresponding target class such that it can be anchored in the actual delegation target. Since templates are defined as regular Java beans, this limitation does not apply to them.

Expansion properties
--------------------

It is possible that a property is defined as a structure for some types whereas it is defined as a regular property for others. In this case an expansion property is defined as a final property on the inner structure:

```java
class Foo {
  Qux value;
  Qux getValue() { return value; }
  void setValue(Qux value) { this.value = value; }
}

class Bar {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

class Qux {
  String value;
  String getValue() { return value; }
  void setValue(String value) { this.value = value; }
}

interface Structure {
  InnerStructure getValue();
  void setValue(InnerStructure value);
}

interface InnerStructure {
  Optional<String> getValue();
  void setValue(String value);
  Optional<String> get();
}

class FooProjection implements Structure {
  final Foo foo;
  FooProjection(Foo foo) { this.foo = foo; }
  @Override InnerStructure getValue() { return new QuxProjection(foo.getValue()); }
  @Override void setValue(InnerStructure value) { foo.setValue(((QuxProjection) value).qux); }
}

class BarProjection implements Structure {
  final Bar bar;
  BarProjection(Bar bar) { this.bar = bar; }
  @Override InnerStructure getValue() { return InnerStructureStringExpansion.of(bar.getValue()); }
  @Override void setValue(InnerStructure value) { bar.setValue(value.get().orElse(null); }
}

class QuxProjection implements InnerStructure {
  final Qux qux;
  QuxProjection(Foo qux) { this.qux = qux; }
  @Override String getValue() { return Optional.ofNullable(foo.getValue()); }
  @Override void setValue(String value) { foo.setValue(value); }
  @Override Optional<String> get() { return Optional.empty(); }
}

class InnerStructureStringExpansion implements InnerStructure {
  final String value;
  static InnerStructure of(String value) { value == null ? null : new InnerStructureStringExpansion(value); }
  private InnerStructureStringExpansion(String value) { this.value = value; }
  @Override Optional<String> getValue() { return Optional.empty(); }
  @Override void setValue(InnerStructure value) { }
  @Override Optional<String> get() { return Optional.of(value); }
}

class Template implements Structure {
  InnerStructure value;
  @Override InnerStructure getValue() { return value; }
  @Override void setValue(InnerStructure value) { this.value = value; }
}

class InnerTemplate implements InnerStructure {
  String value;
  final String expansion;
  InnerTemplate(String expansion) { this.expansion = expansion; }
  @Override Optional<String> getValue() { return Optional.ofNullable(value); }
  @Override void setValue(String value) { this.value = value; }
  @Override Optional<String> get() { return Optional.ofNullable(expansion); }
}
```

The expansion property will never be `null` for an expansion instance. Rather, the wrapper instance will be `null` itself. Templates must define the expansion instance in their constructors.

Normalization
-------------

The structural types implementations attempts to normalize the merged structural hierarchy by discovering overlaps of nested structural types that represent distinct properties. Three forms of normalization are supported:

- intersecting normalization: if two properties are represented by different sets of classes that overlap, both properties will rather be represented by the superset of both type sets which will define a common structural type. This does not apply to non-structured types but for enumerations. (Enabled by default.)
- enumeration normalization: if one out of two enumerations defines a subset of constants of the other enumeration, the less rich enumeration will be represented by the same structural enumeration. (Disabled by default.)
- key-based normalization: it is possible to specify one or several functions that resolve any class to a key values that is used to create merge groups by any key that is shared, for example a class's simple name. (No resolvers are registered by default.)

Enumeration handling
--------------------

Structural enumeration properties are defined as a superset of all enumeration constants for all projected types:

```java
class Foo {
  Qux value;
  Qux getValue() { return value; }
  void setValue(Qux value) { this.value = value; }
}

class Bar {
  Baz value;
  Baz getValue() { return value; }
  void setValue(Baz value) { this.value = value; }
}

enum Qux { VAL1, VAL2 }

class Baz { VAL1, VAL3 }

interface Structure {
  EnumeratedStructure getValue();
  void setValue(EnumeratedStructure value);
}

enum EnumeratedStructure { VAL1, VAL2, VAL3 }

class FooProjection implements Structure {
  final Foo foo;
  FooProjection(Foo foo) { this.foo = foo; }
  @Override EnumeratedStructure getValue() { return EnumeratedStructure.valueOf(foo.getValue().name()); }
  @Override void setValue(EnumeratedStructure value) { foo.setValue(Qux.valueOf(value.name()); }
}

class BarProjection implements Structure {
  final Bar bar;
  BarProjection(Bar bar) { this.bar = bar; }
  @Override EnumeratedStructure getValue() { return EnumeratedStructure.valueOf(bar.getValue().name()); }
  @Override void setValue(EnumeratedStructure value) { bar.setValue(Baz.valueOf(value.name()); }
}

class Template implements Structure {
  EnumeratedStructure value;
  @Override EnumeratedStructure getValue() { return value; }
  @Override void setValue(EnumeratedStructure value) { this.value = value; }
}
```

If an enumeration constant is not supported by a projection, an exception is thrown upon setting the value similar to a failed type conversion.

Additional methods
------------------

Besides getters, setters and owners, additional methods can be defined. By default, all possible methods are generated.

Property-related methods are defined by setting `PropertyGeneration` values:
- `GETTER`: Generates Java bean style getters.
- `ASSUME`: Generates getters for `Optional` types where a `NoSuchElementException` is thrown if the optional property is empty. Assumption requires `GETTER` definition.
- `SETTER`: Generates Java bean style setters.
- `TRIAL`: Generates a setter that catches any exception thrown by type conversion to return it as optional value.
- `FLUENT`: Generates fluent setters that return the current instance.
- `MERGE`: Generates methods that copies and translates values to the appropriate structure.
- `CLEAR`: Generates methods that nullify a value. For primitive types, *0* is set as null.
- `FACTORY`: Generates factory methods that instantiate appropriate instances of nested structures. If `FLUENT` is also defined, a fluent factory method will be added for defining branch properties.
- `OWNER`: Generates ownership methods.

Furthermore, feature methods can be defined by setting `FeatureGeneration` values:
- `FACTORY_ON_STRUCTURE`: Adds convenience factories to any structure for creating projections and templates.
- `COPY`: Generates copy methods that allow copying instances. (Required for `MERGE` property generation.)
- `READ_DELEGATE`: Generates a method that allows unwrapping the delegate of a projection.
- `HASHCODE_EQUALS`: Generates hashCode/equals methods.
- `TO_STRING`: Generates toString methods.

Finally, it is possible to deactivate the creation of structures and/or templates by setting `ImplementationGeneration`.

Additional interfaces
---------------------

It is possible to implement additional interfaces by registering an `InterfaceResolver` that indicates that a structural type should implement additional interfaces. In the most trivial case, this interface does not declare any abstract methods. If abstract methods are however declared, they must match the signature of a method that is declared the implementing structural type. Of course, this is not possible if a structural type refers to other structural types that do not exist at the time that the super interface is created. For this purpose, interfaces can declare type variables that are annotated with the `@StructureReference` annotation. When declaring the super interface, a structural type will then resolve the structure by its name such that the declared methods using this variable match the correct override. For example, if an interface resolver mandates `Structure` to implement an interface `Base`, the following type hierarchy would be realized:

```java
interface Base<@StructureReference("InnerStructure") T> {
  T getValue();
  default void String getValueAsString() { return getValue().toString(); }
}

interface Structure extends Base<InnerStructure> {
  @Override InnerStructure getValue();
  void setValue(InnerStructure value);
}

interface InnerStructure {
  String getValue();
  void setValue(String value);
}
```

If a referenced structure also implements an interface supplied by an `InterfaceResolver`, it is possible to declare this interface as a bound of the annotated type variable.

Maven plugin
------------

Structural types can be created within a project's build using the *structural-type-maven-plugin*. It is set up by configuring a module's build to execute the *structural-type* goal:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype</groupId>
      <artifactId>structural-type-maven-plugin</artifactId>
      <version>LATEST</version>
      <executions>
        <execution>
          <goals>
            <goal>structural-type</goal>
          </goals>
          <configuration>
            <definitions>
              <definition>
                <types>
                  <type>com.acme.MyFoo</type>
                  <type>com.acme.MyBar</type>
                </types>
              </definition>
            </definitions>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Additional interfaces can be implemented by setting the *interfaces* property for a definition where a structure's name is the property key and a list of interface names the property value:

```xml
<configuration>
  <interfaces>
    <com.acme.MyStructure>
      <values>
        <value>java.io.Serializable</value>
      </values>
    </com.acme.MyStructure>
  </interfaces>
</configuration>
```

Finally, it is possible to enable JAXB-specific processing by setting the `<jaxb>JAVAX</jaxb>` (for the *javax* namespace) or `<jaxb>JAKARTA</jaxb>` (for the *jakarta* namespace) configuration. Normalizations can be set by the boolean configurations `normalizeEnumerations` and `normalizeIntersections`. It is furthermore possible to specify a list of `normalizations` where each entry specifies a `pattern` and a `replacement` which are used to resolve to a text-key that is used for key-based normalization. By setting the `subpackage` property to `false`, all generated types are stored in the same package.
