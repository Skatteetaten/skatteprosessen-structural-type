package no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.api.PropertyDefinition;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.mavenplugin.sample.SampleTypedLeafBar;
import no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype.mavenplugin.sample.SampleTypedLeafFoo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class StructuralTypeMojoTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void can_apply_mojo() throws Exception {
        Path root = temporaryFolder.newFolder().toPath();
        Files.copy(StructuralTypeMojoTest.class.getResourceAsStream("/pom.xml"), root.resolve("pom.xml"));
        includeClassFile(root, SampleTypedLeafFoo.class);
        includeClassFile(root, SampleTypedLeafBar.class);
        StructuralTypeMojo mojo = (StructuralTypeMojo) mojoRule.lookupConfiguredMojo(root.toFile(), "structural-type");
        mojo.project.setResolvedArtifacts(Collections.singleton(toArtifact(
            "no.skatteetaten.fastsetting.formueinntekt.felles.structuraltype",
            "structural-type-api",
            "0-SNAPSHOT",
            PropertyDefinition.class
        )));
        mojo.project.setArtifactFilter(new ScopeArtifactFilter("compile"));
        mojo.execute();
        assertThat(root.resolve("target/generated-sources/structural-types")
            .resolve(StructuralTypeMojoTest.class.getPackageName().replace('.', '/'))
            .resolve("sample")).isDirectoryContaining(path -> path.getFileName().toString().equals("SampleTypedLeafStructure.java"));
        assertThat(root.resolve("target/classes")
            .resolve(StructuralTypeMojoTest.class.getPackageName().replace('.', '/'))
            .resolve("sample")).isDirectoryContaining(path -> path.getFileName().toString().equals("SampleTypedLeafStructure.class"));
    }

    private static void includeClassFile(Path root, Class<?> type) throws IOException {
        Path file = root.resolve("target/classes").resolve(type.getName().replace('.', '/') + ".class");
        Files.createDirectories(file.getParent());
        Files.copy(type.getResourceAsStream(type.getSimpleName() + ".class"), file);
    }

    private static Artifact toArtifact(String groupId, String artifiactId, String version, Class<?> hook) {
        DefaultArtifactHandlerStub handler = new DefaultArtifactHandlerStub("jar", "");
        handler.setAddedToClasspath(true);
        ArtifactStub artifact = new ArtifactStub() {
            @Override
            public ArtifactHandler getArtifactHandler() {
                return handler;
            }
        };
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifiactId);
        artifact.setVersion(version);
        artifact.setScope("compile");
        artifact.setFile(new File(hook.getProtectionDomain().getCodeSource().getLocation().getFile()));
        return artifact;
    }
}
