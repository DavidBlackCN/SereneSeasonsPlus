GabousLibs — Multi‑loader Utility Library

Overview
- Reusable library mod with common utilities for Fabric and NeoForge for Minecraft 26.3 (Java 25).
- Built from a multi‑project Gradle setup (common, fabric, neoforge) using Fabric Loom 1.17.21 and NeoGradle 7.1.39.

Build & Publish Locally
- Build all: `./gradlew build`
- Publish to your local Maven: `./gradlew publishAllToMavenLocal`
- Alternatively, publish to a local folder: run `./gradlew publishAllPublicationsToLocalBuildRepoRepository` to write artifacts to `build/maven-repo`.

Coordinates
- Group: `net.Gabou`
- Artifacts:
  - Fabric: `net.Gabou:gaboulibs-fabric:${version}`
  - NeoForge:  `net.Gabou:gaboulibs-neoforge:${version}`

Using in Another Mod
1) Add a repository to resolve the dependency:
   - `mavenLocal()` (for local development)
   - Or `maven { url = uri("<path-to-gaboulibs>/build/maven-repo") }`

2) Add dependency (Architectury/Loom projects):
   - Fabric: `implementation("net.Gabou:gaboulibs-fabric:${version}")`
   - NeoForge:  `implementation("net.Gabou:gaboulibs-neoforge:${version}")`

3) Declare runtime dependency on the library in your mod metadata:
   - Fabric `fabric.mod.json` → under `depends`: `"gaboulibs": "*"`
   - NeoForge `neoforge.mods.toml` → add a dependency block for `gaboulibs` with a version range, e.g. `[1.0,)`.

Composite Build (optional, great during development)
- In the consuming mod's `settings.gradle`: `includeBuild("../GabousLibs")`
- Keep the same coordinates as above; Gradle will substitute from the included build.

Notes
- Minecraft 26.3 uses unobfuscated names; platform jars need no remapping. Sources are packaged in separate sources jars.
- Each platform jar bundles the common module.
