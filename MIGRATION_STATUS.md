# GabouLibs 26.3 migration

Source: xGabou/Gabou-s-Libs, branch `26.2`, commit `b2baf46d4dc4163044c8819f4a72c8a06b4d4b7d` (GabouLibs 1.9).
Target: Minecraft 26.3, Fabric and NeoForge. Upstream attribution and license are preserved.

## Toolchain and implementation

| Component | Version / decision |
| --- | --- |
| Minecraft | 26.3, unobfuscated Mojang names; no Yarn/remapping |
| Java / Gradle | Java 25 / Wrapper 9.6.0 |
| Common and Fabric | Fabric Loom 1.17.21 |
| Fabric Loader / API | 0.19.5 / 0.163.0+26.3 |
| NeoForge / NeoGradle | 26.3.0.76 / userdev 7.1.39 |
| Architectury API | 22.0.5, respective platform implementations |
| Serene Seasons | compile-only CurseMaven file 8938722 |

Platform source sets compile and package the shared source/resources directly. No Architectury platform transformation is needed by this source tree. Maven publications and upload tasks use the complete platform jar; no remote release publication was performed.

`AbilityUtils` uses `Entity.syncVelocity` in place of `hurtMarked`. Target `ServerEntity` consumes this flag to synchronize motion. Item drops use the target API's `Prediction.SERVER_ONLY`, matching vanilla server-side drop usage. Existing public library interfaces and SnowRecord NBT keys are retained. ScreenMixin remains client-only, required, with Java 25 compatibility; no injections were disabled.

## Verification (2026-10-15)

Run from this directory with JDK 25.0.3:

```powershell
.\gradlew.bat build --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1G -Xms128m' --console=plain
.\gradlew.bat :fabric:runClient --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -Xms128m' --console=plain
.\gradlew.bat :neoforge:runClient --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -Xms128m' --console=plain
.\gradlew.bat :neoforge:runServer --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx768m -Xms128m' --console=plain
```

- Full build: PASS (`build-validation-8.log`). All three modules run `checkMigration`: SnowRecord round trips with null/empty/populated distributions, world-reference cleanup, and all four ScreenMixin shadow descriptors/static semantics against actual target classes.
- Fabric and NeoForge clients: PASS startup/resource-loading smoke checks; windows, graphics, sound and texture atlases initialized without reported Mixin errors. Both test windows were closed normally. This does not test every library GUI helper or gameplay ability.
- NeoForge dedicated development server: PASS world initialization (`Done`) and normal shutdown/save. Windows logging emitted an epoll/Linux-native initialization exception while formatting debug output; the server continued and exited successfully. This environment warning is not hidden as a clean log.
- Artifact inspection: both platform jars contain shared AbilityUtils/ScreenMixin classes, no duplicate ZIP entries and no migration-test classes. Separate sources jars generated.
- Fabric integration with SSP: initialization reached GabouLibs 1.9 and elected `SERENE_SEASONS_PLUS`; server then stopped at first-launch EULA handling. No full joint world test was completed.
- NeoForge integration with SSP: FAIL in SSP metadata, which declares GabouLibs `versionRange = ""`. Loader identifies GabouLibs 1.9 but rejects that empty range. This requires a separate correction in the `26.3` SSP directory; it is not fixed by changing GabouLibs' version.
- SSP client integration: blocked by its development DevAuth login configuration. Standalone GabouLibs client checks above do not require DevAuth.
- Earlier native-memory allocation failures were environment failures; the subsequent complete build passed after memory became available.

Local runtime logs are under `build/validation/` and are intentionally not committed. Fabric joint server evidence is in sibling `26.3/build/reports/migration/gaboulibs-fabric-server.log`.

## Code review

Code Review: PASS WITH RISKS for the GabouLibs migration.

- Reviewed all source, build, metadata, test and documentation changes; target is this directory only. Sibling SSP source is unchanged.
- No GabouLibs build or startup blocker found. Version metadata, client/server separation, API descriptors, packaging and retained NBT format checked.
- `git diff --check` against the upstream baseline: PASS. The orphan root commit also includes inherited trailing whitespace/blank EOF lines in upstream files; these were retained to avoid unrelated formatting changes. Update Notes: PASS; migration and API adaptation entries included.
- Remaining coverage: multiplayer packet exchange, in-world ability behavior, optional Serene Seasons gameplay and complete SSP integration. NeoForge SSP metadata and DevAuth issues above remain external integration blockers.
- Baseline Fabric metadata references a missing optional icon; retained as a cosmetic upstream issue. Gradle reports deprecated plugin behavior ahead of Gradle 10; Wrapper remains pinned to tested 9.6.0.
