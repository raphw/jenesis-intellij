# Jenesis for IntelliJ IDEA

An IntelliJ IDEA plugin for [Jenesis](https://jenesis.build) builds: it loads the modules of a build into the
IDE and runs its selectors.

- **Loading a build.** Opening a project whose folder carries `build/jenesis/Make.java` offers to load its
  modules. The plugin runs `java build/jenesis/Make.java ide/idea` with the tests skipped, which writes one IDE
  module per module of the build - its sources, tests, generated sources and resolved libraries - into
  `.idea/modules.xml` and an `.iml` file per module. The IDE then reloads them.
- **Loading it again.** A banner on a `pom.xml`, a `module-info.java`, a `jenesis.properties`,
  `jenesis.plugins.properties` or a `.properties` file of a `build.jenesis` folder offers to load the build
  again once the file changed after the last load. **Tools | Load Jenesis Build** does so at any time.
- **Running selectors.** A **Jenesis** run configuration runs `Make.java` with any selectors (`stage`,
  `+module/test`, none for a full build) and `-Djenesis.*` settings, with the output in the run console.

Jenesis runs on JDK 25 or newer. The plugin takes the Java home a run configuration names, or else the project
SDK, any other configured JDK or `JAVA_HOME`, the first of them that is JDK 25 or newer. Where none is, it
says which ones it found.

The plugin needs IntelliJ IDEA 2025.2 or newer, with the bundled Java plugin.

## Installing

Build the plugin and install the jar it stages:

```bash
java build/jenesis/Make.java stage
```

Then choose **Settings | Plugins | ⚙ | Install Plugin from Disk...** and pick
`target/stage/maven/output/codes/rafael/jenesis/intellij-plugin/0.1.0/intellij-plugin-0.1.0.jar`.

## Building

The project is a Jenesis build itself, the Maven artifact `codes.rafael.jenesis:intellij-plugin`, with the
tool vendored under `build/jenesis/`. It needs JDK 25 or newer and nothing else:

```bash
java build/jenesis/Make.java
```

The plugin compiles at release 21, which is the Java release IntelliJ IDEA 2025.2 runs on. The IntelliJ
platform is a `provided` dependency on the platform's own artifacts, each with every transitive dependency
excluded, as the IDE supplies them. `intellij.version` in `pom.xml` names the platform release it compiles
against.

`jenesis.properties` names the JetBrains repository for the group `com.jetbrains.intellij`, ahead of Maven
Central. A `jenesis.maven.uri` in your own `~/.jenesis/jenesis.properties` outranks it. If you keep one, add
the same entry to it, or pass the project's chain on the command line:

```bash
java -Djenesis.maven.uri="$(sed -n 's/^jenesis.maven.uri=//p' jenesis.properties)" build/jenesis/Make.java
```

Every dependency is pinned in `pom.xml`. After changing one, run `java build/jenesis/Make.java pin` and commit
the rewritten lines, so that `-Djenesis.dependency.pin=strict` builds.

## Layout

- `src/main/java`: `JenesisBuild` holds what the plugin knows of a build without the IDE - where `Make.java`
  is, which files shape the modules, which JDK runs it and the command line. The other classes connect it to
  the IDE: `JenesisLoader` runs `ide/idea`, `JenesisStartup` offers the first load, `StaleBanner` and
  `BuildFileListener` offer the next, `LoadAction` is the menu entry, and `JenesisConfigurationType` and its
  classes are the run configuration.
- `src/main/resources/META-INF/plugin.xml` declares all of them.
- `src/test/java`: `JenesisBuildTest` covers `JenesisBuild`. `PluginDescriptorTest` holds `plugin.xml` to the
  classes, icons and version it names.
