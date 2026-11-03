package codes.rafael.jenesis.intellij;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JenesisBuildTest {

    @TempDir
    private Path root;

    @Test
    public void finds_a_build_that_carries_make() throws IOException {
        make(root);

        assertThat(JenesisBuild.of(root)).contains(new JenesisBuild(root.toAbsolutePath().normalize()));
    }

    @Test
    public void finds_no_build_without_make() {
        assertThat(JenesisBuild.of(root)).isEmpty();
        assertThat(JenesisBuild.of(null)).isEmpty();
    }

    @Test
    public void reads_the_version_of_the_vendored_tool() throws IOException {
        JenesisBuild build = make(root);
        Files.writeString(root.resolve("build/jenesis/jenesis.version"), "0.18.1\n");

        assertThat(build.version()).contains("0.18.1");
    }

    @Test
    public void defines_the_files_that_shape_the_build() throws IOException {
        JenesisBuild build = make(root);

        assertThat(build.defines(root.resolve("pom.xml"))).isTrue();
        assertThat(build.defines(root.resolve("library/src/main/java/module-info.java"))).isTrue();
        assertThat(build.defines(root.resolve("jenesis.properties"))).isTrue();
        assertThat(build.defines(root.resolve("jenesis.plugins.properties"))).isTrue();
        assertThat(build.defines(root.resolve("build.jenesis/checkstyle.properties"))).isTrue();
        assertThat(build.defines(root.resolve("library/build.jenesis/process-javac.properties"))).isTrue();
    }

    @Test
    public void defines_no_source_output_or_vendored_file() throws IOException {
        JenesisBuild build = make(root);

        assertThat(build.defines(root.resolve("src/main/java/sample/Sample.java"))).isFalse();
        assertThat(build.defines(root.resolve("build.jenesis/checkstyle.xml"))).isFalse();
        assertThat(build.defines(root.resolve("target/build/pom.xml"))).isFalse();
        assertThat(build.defines(root.resolve("library/target/pom.xml"))).isFalse();
        assertThat(build.defines(root.resolve(".idea/modules.xml"))).isFalse();
        assertThat(build.defines(root.resolve("build/jenesis/Project.java"))).isFalse();
        assertThat(build.defines(root.getParent().resolve("pom.xml"))).isFalse();
        assertThat(build.defines(root)).isFalse();
    }

    @Test
    public void is_loaded_once_the_modules_and_the_stamp_are_written() throws IOException {
        JenesisBuild build = make(root);
        assertThat(build.loaded()).isFalse();

        Files.createDirectories(root.resolve(".idea"));
        Files.writeString(root.resolve(".idea/modules.xml"), "<project/>");
        assertThat(build.loaded()).as("modules.xml alone may be the IDE's own").isFalse();

        build.markLoaded();
        assertThat(build.loaded()).isTrue();
    }

    @Test
    public void is_stale_for_a_build_file_until_loaded_and_again_once_it_changes() throws IOException {
        JenesisBuild build = make(root);
        Path pom = Files.writeString(root.resolve("pom.xml"), "<project/>");
        Path source = Files.createDirectories(root.resolve("src/main/java")).resolve("Sample.java");
        Files.writeString(source, "class Sample { }");
        assertThat(build.stale(pom)).isTrue();

        Files.createDirectories(root.resolve(".idea"));
        Files.writeString(root.resolve(".idea/modules.xml"), "<project/>");
        build.markLoaded();
        Instant loaded = Files.getLastModifiedTime(build.stamp()).toInstant();
        Files.setLastModifiedTime(pom, FileTime.from(loaded.minusSeconds(60)));
        assertThat(build.stale(pom)).isFalse();

        Files.setLastModifiedTime(pom, FileTime.from(loaded.plusSeconds(60)));
        Files.setLastModifiedTime(source, FileTime.from(loaded.plusSeconds(60)));
        assertThat(build.stale(pom)).isTrue();
        assertThat(build.stale(source)).as("a source is compiled, it does not shape the modules").isFalse();
    }

    @Test
    public void runs_make_with_its_settings_before_the_selectors() throws IOException {
        JenesisBuild build = make(root);
        Path javaHome = root.resolve("jdk");

        assertThat(build.command(javaHome, List.of("-Djenesis.test.skip=true"), List.of("ide/idea", "+module")))
                .containsExactly(JenesisBuild.java(javaHome).toString(),
                        "-Djenesis.test.skip=true",
                        "build/jenesis/Make.java",
                        "ide/idea",
                        "+module");
    }

    @Test
    public void refuses_a_setting_that_is_not_a_property() throws IOException {
        JenesisBuild build = make(root);

        assertThatThrownBy(() -> build.command(root, List.of("jenesis.test.skip=true"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jenesis.test.skip=true")
                .hasMessageContaining("-Djenesis.<group>.<key>=<value>");
    }

    @Test
    public void reads_the_feature_release_of_a_jdk() throws IOException {
        assertThat(JenesisBuild.release(jdk("jdk-25", "25.0.4"))).contains(25);
        assertThat(JenesisBuild.release(jdk("jdk-26", "26"))).contains(26);
        assertThat(JenesisBuild.release(jdk("jdk-8", "1.8.0_402"))).contains(8);
        assertThat(JenesisBuild.release(root.resolve("none"))).isEmpty();
    }

    @Test
    public void picks_the_first_jdk_that_runs_jenesis() throws IOException {
        Path old = jdk("jdk-21", "21.0.5"), current = jdk("jdk-25", "25.0.4"), newer = jdk("jdk-26", "26");

        assertThat(JenesisBuild.javaHome(List.of(old, current, newer))).isEqualTo(current);
    }

    @Test
    public void names_the_jdks_found_and_the_fix_when_none_runs_jenesis() throws IOException {
        Path old = jdk("jdk-21", "21.0.5");

        assertThatThrownBy(() -> JenesisBuild.javaHome(List.of(old, root.resolve("missing"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(old + " (Java 21)")
                .hasMessageContaining("missing (no JDK)")
                .hasMessageContaining("JDK 25 or newer");
        assertThatThrownBy(() -> JenesisBuild.javaHome(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no JDK is configured");
    }

    @Test
    public void splits_a_line_into_words_keeping_quoted_spaces() {
        assertThat(JenesisBuild.words(" stage  +module/test ")).containsExactly("stage", "+module/test");
        assertThat(JenesisBuild.words("-Djenesis.test.filter='.*Sample Test' \"-Da=b c\""))
                .containsExactly("-Djenesis.test.filter=.*Sample Test", "-Da=b c");
        assertThat(JenesisBuild.words("''")).containsExactly("");
        assertThat(JenesisBuild.words(null)).isEmpty();
        assertThatThrownBy(() -> JenesisBuild.words("stage 'open"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unclosed quote");
    }

    private static JenesisBuild make(Path root) throws IOException {
        Path make = root.resolve(JenesisBuild.MAKE);
        Files.createDirectories(make.getParent());
        Files.writeString(make, "public class Make { }");
        return JenesisBuild.of(root).orElseThrow();
    }

    private Path jdk(String name, String version) throws IOException {
        Path home = root.resolve(name);
        Path java = JenesisBuild.java(home);
        Files.createDirectories(java.getParent());
        Files.writeString(java, "");
        Files.writeString(home.resolve("release"), "IMPLEMENTOR=\"Sample\"\nJAVA_VERSION=\"" + version + "\"\n");
        return home;
    }
}
