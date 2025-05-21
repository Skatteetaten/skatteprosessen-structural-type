package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

public class InMemoryCompiler implements Function<Map<ClassName, JavaFile>, List<Class<?>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryCompiler.class);

    private final JavaCompiler compiler;

    public InMemoryCompiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    @Override
    public List<Class<?>> apply(Map<ClassName, JavaFile> types) {
        String classDumpFolder = System.getProperty("no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.generator.dump");
        if (classDumpFolder != null) {
            LOGGER.info("Dumping {} generated Java source files to {}", types.size(), classDumpFolder);
            types.values().forEach(file -> {
                try {
                    file.writeTo(new File(classDumpFolder));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            });
        }
        List<JavaFileObject> files = types.entrySet().stream()
            .peek(entry -> LOGGER.debug("{}.java\n{}", entry.getKey(), entry.getValue()))
            .map(entry -> entry.getValue().toJavaFileObject())
            .collect(Collectors.toList());

        Map<String, InMemoryJavaFileObject> targets = new HashMap<>();
        try (JavaFileManager manager = new CapturingFileManager(
            compiler.getStandardFileManager(null, null, null),
            targets
        )) {
            if (!compiler.getTask(new ConsumingWriter(LOGGER::info), manager, null, null, null, files).call()) {
                throw new AssertionError("Error during compilation");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ClassLoader classLoader = new InMemoryClassLoader(targets);
        return types.keySet().stream().map(name -> {
            try {
                return Class.forName(name.toString(), false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }).collect(Collectors.toList());
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
        public void flush() { }

        @Override
        public void close() { }
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InMemoryJavaFileObject(String className) throws URISyntaxException {
            super(new URI(null, null, className, null), Kind.CLASS);
        }

        @Override
        public String getName() { return uri.getRawSchemeSpecificPart(); }

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

    static class InMemoryClassLoader extends ClassLoader {

        private final Map<String, InMemoryJavaFileObject> targets;

        InMemoryClassLoader(Map<String, InMemoryJavaFileObject> targets) {
            this.targets = targets;
        }

        @Override
        protected Class<?> findClass(String className) throws ClassNotFoundException {
            InMemoryJavaFileObject target = targets.get(className);
            if (target != null) {
                byte[] bytes = target.toByteArray();
                return defineClass(className, bytes, 0, bytes.length);
            }
            return super.findClass(className);
        }
    }
}
