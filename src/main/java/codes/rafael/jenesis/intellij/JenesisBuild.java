package codes.rafael.jenesis.intellij;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record JenesisBuild(Path root) {

    public static final String MAKE = "build/jenesis/Make.java", LOADER = "ide/idea";
    public static final int RELEASE = 25;
    private static final Set<String> BUILD_FILES = Set.of("pom.xml",
            "module-info.java",
            "jenesis.properties",
            "jenesis.plugins.properties",
            "jenesis.plugins.arguments.properties");
    private static final Set<String> CONFIGURATION_FOLDERS = Set.of("build.jenesis");
    private static final Pattern VERSION = Pattern.compile("JAVA_VERSION=\"?(?:1\\.)?(\\d+)\\D?.*");

    public static Optional<JenesisBuild> of(Path root) {
        return root != null && Files.isRegularFile(root.resolve(MAKE))
                ? Optional.of(new JenesisBuild(root.toAbsolutePath().normalize()))
                : Optional.empty();
    }

    public Path stamp() {
        return root.resolve(".idea").resolve("jenesis.loaded");
    }

    public boolean loaded() {
        return Files.isRegularFile(stamp()) && Files.isRegularFile(root.resolve(".idea").resolve("modules.xml"));
    }

    public void markLoaded() {
        try {
            Files.createDirectories(stamp().getParent());
            Files.writeString(stamp(), version().orElse("unknown") + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not record the load of the Jenesis build in " + stamp(), e);
        }
    }

    public Optional<String> version() {
        Path file = root.resolve("build/jenesis/jenesis.version");
        try {
            return Files.isRegularFile(file)
                    ? Optional.of(Files.readString(file, StandardCharsets.UTF_8).strip()).filter(value -> !value.isEmpty())
                    : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the Jenesis version from " + file, e);
        }
    }

    public boolean defines(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)
                || normalized.startsWith(root.resolve(".idea"))
                || normalized.startsWith(root.resolve(MAKE).getParent())) {
            return false;
        }
        Path relative = root.relativize(normalized);
        if (relative.getNameCount() == 0) {
            return false;
        }
        for (Path segment : relative) {
            if (segment.toString().equals("target")) {
                return false;
            }
        }
        String name = normalized.getFileName().toString();
        Path parent = relative.getParent();
        return BUILD_FILES.contains(name)
                || parent != null && CONFIGURATION_FOLDERS.contains(parent.getFileName().toString())
                && name.endsWith(".properties");
    }

    public boolean stale(Path file) {
        if (!defines(file)) {
            return false;
        }
        if (!loaded()) {
            return true;
        }
        try {
            FileTime changed = Files.getLastModifiedTime(file), loaded = Files.getLastModifiedTime(stamp());
            return changed.compareTo(loaded) > 0;
        } catch (IOException e) {
            return true;
        }
    }

    public List<String> command(Path javaHome, Collection<String> properties, Collection<String> selectors) {
        List<String> command = new ArrayList<>();
        command.add(java(javaHome).toString());
        for (String property : properties) {
            if (!property.startsWith("-D")) {
                throw new IllegalArgumentException("Not a -D<key>=<value> property: " + property
                        + " - write it as -Djenesis.<group>.<key>=<value>");
            }
            command.add(property);
        }
        command.add(MAKE);
        command.addAll(selectors);
        return command;
    }

    public static Path java(Path javaHome) {
        return javaHome.resolve("bin").resolve(System.getProperty("os.name", "").startsWith("Windows")
                ? "java.exe"
                : "java");
    }

    public static Optional<Integer> release(Path javaHome) {
        Path file = javaHome.resolve("release");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = VERSION.matcher(line.strip());
                if (matcher.matches()) {
                    return Optional.of(Integer.parseInt(matcher.group(1)));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Path javaHome(List<Path> candidates) {
        List<String> rejected = new ArrayList<>();
        for (Path candidate : candidates) {
            Optional<Integer> release = release(candidate);
            if (release.isPresent() && release.get() >= RELEASE && Files.isRegularFile(java(candidate))) {
                return candidate;
            }
            rejected.add(candidate + release.map(value -> " (Java " + value + ")").orElse(" (no JDK)"));
        }
        throw new IllegalStateException("Jenesis runs on JDK " + RELEASE + " or newer, "
                + (rejected.isEmpty() ? "but no JDK is configured" : "but found only " + String.join(", ", rejected))
                + ": set the project SDK, the Java home of the run configuration or JAVA_HOME to JDK "
                + RELEASE + " or newer");
    }

    public static List<String> words(String line) {
        List<String> words = new ArrayList<>();
        if (line == null) {
            return words;
        }
        StringBuilder word = new StringBuilder();
        boolean started = false;
        char quote = 0;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    word.append(character);
                }
            } else if (character == '"' || character == '\'') {
                quote = character;
                started = true;
            } else if (Character.isWhitespace(character)) {
                if (started) {
                    words.add(word.toString());
                    word.setLength(0);
                    started = false;
                }
            } else {
                word.append(character);
                started = true;
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Unclosed quote " + quote + " in: " + line);
        }
        if (started) {
            words.add(word.toString());
        }
        return words;
    }
}
