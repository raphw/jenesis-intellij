# AGENTS.md

The IntelliJ IDEA plugin for Jenesis builds, built by Jenesis itself as `codes.rafael.jenesis:intellij-plugin`.
`README.md` covers what it does, installing, building and the layout.

## Build & test

- **JDK 25 or newer.** `java build/jenesis/Make.java` builds and tests; `stage` writes the installable jar to
  `target/stage/maven/output/codes/rafael/jenesis/intellij-plugin/<version>/`.
- A `jenesis.maven.uri` in `~/.jenesis/jenesis.properties` outranks the project's, which names the JetBrains
  repository; pass the project's on the command line where yours lacks it (`README.md` shows how).
- Dependencies are pinned in `pom.xml`: after changing one, run `pin` and commit the rewritten lines.
- `build/jenesis/` is the vendored tool. It is replaced whole by its installer
  (`curl -fsSL https://get.jenesis.build | bash`), never edited.

## How the code is written

- Java at release 21, the release the oldest supported IDE runs on: no `import module`, no `_`.
- What can be decided without the IDE lives in `JenesisBuild`, which imports no platform class and is unit
  tested; the classes that touch the platform stay thin and delegate to it.
- The platform is `provided`, one dependency per platform artifact with every transitive dependency
  excluded. An artifact is added for a class the compiler asks for, never as a convenience. `since-build` in
  `plugin.xml` matches the line `intellij.version` names.
- `plugin.xml` and `pom.xml` name one version, and `PluginDescriptorTest` holds every class, icon and
  notification group `plugin.xml` names to the code.
- No comments, no Javadoc; a test method is a sentence in `snake_case`.
- Fail loudly, naming the fix: a missing JDK, a malformed setting or a missing `Make.java` is a message saying
  what was found and what to do.
