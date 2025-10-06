package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.mavenplugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.CompoundOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.EnumerationOf;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.*;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "structural-type",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class StructuralTypeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}", readonly = true)
    public String classes;

    @Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/structural-types")
    public String sources;

    @Parameter(required = true)
    public List<StructuralTypeDefinition> definitions;

    @Parameter
    public Map<String, InterfaceDefintion> interfaces = Collections.emptyMap();

    @Parameter
    public List<TranslationDefinition> translations = Collections.emptyList();

    @Parameter
    public List<RenamingDefinition> renamings = Collections.emptyList();

    @Parameter(required = true, defaultValue = "true")
    public boolean subpackage;

    @Parameter
    public JaxbHandler jaxb;

    @Parameter(required = true, defaultValue = "true")
    public boolean richEnumerations;

    @Parameter(required = true, defaultValue = "true")
    public boolean subtyping;

    @Parameter(required = true, defaultValue = "true")
    public boolean normalizeIntersections;

    @Parameter(required = true, defaultValue = "false")
    public boolean normalizeEnumerations;

    @Parameter(required = true, defaultValue = "false")
    public boolean normalizeSimpleNames;

    @Parameter(required = true, defaultValue = "true")
    public boolean discover;

    @Parameter(required = true, defaultValue = "false")
    public boolean exceptionOnEmptySetter;

    @Parameter
    public List<NormalizationDefinition> normalizations = Collections.emptyList();

    @Parameter
    public List<FilterDefinition> filters = Collections.emptyList();

    @Parameter
    public List<NamingDefinition> namings;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        getLog().debug("Compiling classes using " + compiler.name());
        File classes = new File(this.classes), sources = new File(this.sources);
        if (!classes.isDirectory() && !classes.mkdirs()) {
            throw new MojoFailureException("Not a directory: " + classes.getAbsolutePath());
        } else if (!sources.isDirectory() && !sources.mkdirs()) {
            throw new MojoFailureException("Not a directory: " + sources.getAbsolutePath());
        }
        List<String> elements;
        try {
            elements = new ArrayList<>(project.getCompileClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Could not resolve compile class path", e);
        }
        getLog().debug("Resolving class path: " + elements);
        List<URL> classPath = new ArrayList<>();
        for (String element : elements) {
            File file = new File(element);
            if (!file.exists()) {
                throw new MojoExecutionException("Class path element does not exist: " + element);
            }
            try {
                classPath.add(file.toURI().toURL());
            } catch (Exception e) {
                throw new MojoExecutionException("Could not resolve class path element: " + element, e);
            }
        }
        try (URLClassLoader classLoader = new URLClassLoader(
            classPath.toArray(URL[]::new),
            ClassLoader.getPlatformClassLoader()
        ) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.startsWith("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.")) {
                    return Class.forName(name);
                }
                return super.findClass(name);
            }
        }) {
            List<? extends Class<?>> structures;
            if (discover) {
                try (ScanResult result = new ClassGraph().addClassLoader(classLoader).filterClasspathElements(
                    element -> !element.startsWith(classes.getAbsolutePath())
                ).enableClassInfo().enableAnnotationInfo().scan()) {
                    structures = Stream.concat(
                        result.getClassesWithAnnotation(CompoundOf.class.getTypeName()).stream(),
                        result.getClassesWithAnnotation(EnumerationOf.class.getTypeName()).stream()
                    ).map(ClassInfo::loadClass).collect(Collectors.toList());
                }
                getLog().info("Discovered " + structures.size() + " structural types as predefinitions");
                if (!structures.isEmpty()) {
                    getLog().debug("Discovered structures:" + structures.stream()
                        .map(structure -> "\n - " + structure.getTypeName())
                        .sorted()
                        .collect(Collectors.joining()));
                }
            } else {
                structures = Collections.emptyList();
                getLog().debug("Structural type discovery is disabled");
            }
            List<BiPredicate<Class<?>, String>> exclusions = filters.stream()
                .map(filter -> Map.entry(
                    Pattern.compile(filter.type).asMatchPredicate(),
                    filter.properties.stream().map(property -> Pattern.compile(property).asMatchPredicate()).collect(Collectors.toList())
                ))
                .map(entry -> (BiPredicate<Class<?>, String>) ((type, property) -> entry.getKey().test(type.getTypeName()) && entry.getValue().stream().anyMatch(
                    pattern -> pattern.test(property))
                ))
                .collect(Collectors.toList());
            getLog().debug("Compiled " + exclusions.size() + " conditions for property exclusion");
            Map<Pattern, Integer> namings = this.namings == null ? Collections.emptyMap() : this.namings.stream().collect(Collectors.toMap(
                subordination -> Pattern.compile(subordination.pattern),
                subordination -> subordination.priority,
                Math::min,
                LinkedHashMap::new
            ));

            @SuppressWarnings("unchecked")
            StructuralType structuralType = new StructuralType()
                .withNormalizedIntersections(normalizeIntersections)
                .withNormalizedEnumerations(normalizeEnumerations)
                .withNormalizedKeys(normalizations.stream()
                    .map(normalization -> Map.entry(Pattern.compile(normalization.pattern), normalization.replacement))
                    .map(entry -> (Function<Class<?>, String>) type -> entry.getKey().matcher(type.getName()).replaceAll(entry.getValue()))
                    .toArray(Function[]::new))
                .withPredefinitions(structures.toArray(Class<?>[]::new))
                .withExceptionOnEmptySetter(exceptionOnEmptySetter)
                .withCondition((type, property) -> exclusions.stream().noneMatch(exclusion -> exclusion.test(type, property)))
                .withNamingStrategy(DecoratingNamingStrategy.withDuplicationResolution(DecoratingNamingStrategy.withReplacements(
                    new PrioritizingNamingStrategy(
                        new CommonPrefixNamingStrategy(1, subpackage),
                        type -> namings.entrySet().stream()
                            .filter(entry -> entry.getKey().matcher(type.getTypeName()).matches()).findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(0)
                    ),
                    renamings == null ? Collections.emptyMap() : renamings.stream().collect(Collectors.toMap(
                        renaming -> Pattern.compile(renaming.pattern),
                        renaming -> renaming.replacement
                    ))
                )));
            if (jaxb != null) {
                getLog().debug("Processing JAXB annotations for namespace " + jaxb + " to build structural types");
                try {
                    structuralType = structuralType.withStructuralResolver(
                        (jaxb == JaxbHandler.JAVAX
                            ? JaxbStructuralResolver.ofJavax(classLoader)
                            : JaxbStructuralResolver.ofJakarta(classLoader)).withSubtyping(subtyping).withEnumerations(richEnumerations)
                    ).withAccessResolver(
                        jaxb == JaxbHandler.JAVAX
                            ? JaxbFallbackBeanAccessResolver.ofJavax(new BeanAccessResolver(true, true), classLoader)
                            : JaxbFallbackBeanAccessResolver.ofJakarta(new BeanAccessResolver(true, true), classLoader)
                    ).withGrouper(
                        jaxb == JaxbHandler.JAVAX
                            ? JaxbSimpleTypeNameGrouper.ofJavax(classLoader)
                            : JaxbSimpleTypeNameGrouper.ofJakarta(classLoader)
                    ).withTypeResolver(
                        new SimpleTypeResolver(jaxb == JaxbHandler.JAVAX
                            ? SimpleTypeResolver.EnumHandler.UsingJaxb.ofJavax(classLoader)
                            : SimpleTypeResolver.EnumHandler.UsingJaxb.ofJakarta(classLoader))
                    );
                } catch (Exception e) {
                    throw new MojoFailureException("Could not resolve JAXB context", e);
                }
            }
            if (normalizeSimpleNames) {
                if (jaxb != null) {
                    structuralType = structuralType.withNormalizedKeys(
                        jaxb == JaxbHandler.JAVAX
                            ? JaxbSimpleTypeKeyResolver.ofJavax(classLoader)
                            : JaxbSimpleTypeKeyResolver.ofJakarta(classLoader)
                    );
                } else {
                    structuralType = structuralType.withNormalizedKeys(Class::getSimpleName);
                }
            }
            getLog().debug("Resolving additional interfaces");
            Map<String, List<Class<?>>> interfacesByName = new HashMap<>();
            for (Map.Entry<String, InterfaceDefintion> entry : interfaces.entrySet()) {
                List<Class<?>> resolved = new ArrayList<>();
                for (String type : entry.getValue().values) {
                    try {
                        resolved.add(Class.forName(type, true, classLoader));
                    } catch (ClassNotFoundException e) {
                        throw new MojoFailureException("Could not find class on class path: " + type, e);
                    }
                }
                interfacesByName.put(entry.getKey(), resolved);
            }
            structuralType = structuralType.withInterfaceResolver((structure, components) -> {
                List<Class<?>> resolution = interfacesByName.getOrDefault(structure.toString(), Collections.emptyList());
                if (!resolution.isEmpty()) {
                    getLog().debug("Implementing additional interfaces " + resolution + " for " + structure);
                }
                return resolution;
            });
            getLog().debug("Resolved additional interfaces");
            getLog().debug("Resolving property translations");
            Map<Class<?>, Map<String, String>> translationByClass = new HashMap<>();
            for (TranslationDefinition translation : translations) {
                try {
                    if (translationByClass.putIfAbsent(
                        Class.forName(translation.type, true, classLoader),
                        translation.values
                    ) != null) {
                        throw new IllegalStateException("Duplicate translation definition for " + translation.type);
                    }
                } catch (ClassNotFoundException e) {
                    throw new MojoFailureException("Could not find class on class path: " + translation.type, e);
                }
            }
            structuralType = structuralType.withNodeResolver(new NormalizingNodeResolver((type, property) -> translationByClass
                .getOrDefault(type, Collections.emptyMap())
                .getOrDefault(property, property)));
            getLog().debug("Resolved property translations");
            for (StructuralTypeDefinition definition : definitions) {
                if (definition.types.isEmpty()) {
                    getLog().warn("Skipping definition without types.");
                    continue;
                }
                getLog().info("Creating structural types for:\n - " + String.join("\n - ", definition.types));
                List<Class<?>> types = new ArrayList<>(definition.types.size());
                for (String type : definition.types) {
                    try {
                        types.add(Class.forName(type, true, classLoader));
                    } catch (ClassNotFoundException e) {
                        throw new MojoFailureException("Could not find class on class path: " + type, e);
                    }
                }
                getLog().debug("Resolved all input classes for structural types");
                Map<ClassName, JavaFile> result = structuralType.make(types);
                if (result.isEmpty()) {
                    getLog().warn("Did not create any structural types since all types are predefined");
                    return;
                }
                for (Map.Entry<ClassName, JavaFile> entry : result.entrySet()) {
                    try {
                        entry.getValue().writeTo(sources);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Could not write class " + entry.getKey(), e);
                    }
                }
                getLog().debug("Successfully wrote structural types sources to " + sources.getAbsolutePath());
                Map<String, InMemoryJavaFileObject> targets = new HashMap<>();
                try (JavaFileManager manager = new CapturingFileManager(
                    compiler.getStandardFileManager(null, null, null),
                    targets
                )) {
                    if (!compiler.getTask(
                        new ConsumingWriter(getLog()::debug), manager,
                        null,
                        List.of("-classpath", String.join(File.pathSeparator, elements)),
                        null,
                        result.values().stream().map(JavaFile::toJavaFileObject).collect(Collectors.toList())
                    ).call()) {
                        throw new MojoFailureException("Failed to compile structural types for "
                            + definition.types
                            + " - did you remember to include the API module in the dependencies?");
                    }
                } catch (IOException e) {
                    throw new MojoFailureException("Could not compile classes", e);
                }
                getLog().debug("Successfully compiled structural types");
                for (Map.Entry<String, InMemoryJavaFileObject> entry : targets.entrySet()) {
                    File file = new File(classes, entry.getKey().replace('.', '/') + ".class");
                    if (!file.getParentFile().mkdirs() && !file.getParentFile().isDirectory()) {
                        throw new MojoExecutionException("Cannot create folder: " + file.getParent());
                    }
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(entry.getValue().toByteArray());
                    } catch (IOException e) {
                        throw new MojoFailureException("Could not write class file for " + entry.getKey(), e);
                    }
                }
                getLog().info("Successfully wrote structural types: " + targets.size() + " source files compiled");
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to close class loader", e);
        }
        if (definitions.isEmpty()) {
            getLog().warn("No structural type definitions were found");
        } else {
            project.addCompileSourceRoot(sources.getAbsolutePath());
        }
    }

    static class ConsumingWriter extends Writer {

        private final Consumer<String> consumer;

        ConsumingWriter(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            consumer.accept(new String(cbuf, off, len));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InMemoryJavaFileObject(String className) throws URISyntaxException {
            super(new URI(null, null, className, null), Kind.CLASS);
        }

        @Override
        public String getName() {
            return uri.getRawSchemeSpecificPart();
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }

        byte[] toByteArray() {
            return outputStream.toByteArray();
        }
    }

    static class CapturingFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final Map<String, InMemoryJavaFileObject> targets;

        CapturingFileManager(StandardJavaFileManager target, Map<String, InMemoryJavaFileObject> targets) {
            super(target);
            this.targets = targets;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
            Location location, String className,
            JavaFileObject.Kind kind, FileObject sibling
        ) {
            InMemoryJavaFileObject target;
            try {
                target = new InMemoryJavaFileObject(className);
            } catch (URISyntaxException e) {
                throw new AssertionError(e);
            }
            targets.put(className, target);
            return target;
        }
    }
}
