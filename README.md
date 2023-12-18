### `external-schema-demo`

This is a demonstration of interpreting Restricted DSL files using a schema exported
from a Gradle build.

> ⚠️ If cloning this repository, please also clone the submodules recursively:
> ```shell
> git clone --recurse-submodules https://github.com/h0tk3y/external-schema-demo
> ```

### Building

The command line application is in [`main.kt`](src/main/kotlin/main.kt). 

To build a distribution in `build/install/external-schema-demo`, run:

```shell
./gradlew installDist
```

### Usage
1. Run a Gradle build that exports restricted DSL schemas for settings and projects

    The Restricted DSL schemas may be found under `.gradle/restricted-schema/*.something.schema` in the root 
    project directory and in the subprojects.

    Note that schemas are only exported if a settings file or a project build file uses
    the restricted DSL (`*.gradle.something`).

2. Run this demo application with the following command line options:

    * `--script <file>` pointing to a `settings.gradle.something` or `build.gradle.something` file;
    * `--schema <file>` pointing to an exported schema file produced by the previous step
   
3. The resulting output is the content of the top-level-receiver. 
   Starting from the top level, each object has its properties and _added objects_ printed recursively.

### Example output

```
Settings#0 {
    dependencyResolutionManagement = DependencyResolutionManagement {
        repositories = RepositoryHandler {
            + added MavenArtifactRepository#10 from (top-level-object).dependencyResolutionManagement_default.repositories_default.google#10()
            + added MavenArtifactRepository#11 from (top-level-object).dependencyResolutionManagement_default.repositories_default.mavenCentral#11()
        }
    }
    pluginManagement = PluginManagementSpec {
        repositories = RepositoryHandler {
            + added MavenArtifactRepository#5 from (top-level-object).pluginManagement_default.repositories_default.google#5()
            + added MavenArtifactRepository#6 from (top-level-object).pluginManagement_default.repositories_default.mavenCentral#6()
            + added ArtifactRepository#7 from (top-level-object).pluginManagement_default.repositories_default.gradlePluginPortal#7()
        }
        + added by call: (top-level-object).pluginManagement_default.includeBuild#3(rootProject = "build-logic")
    }
    rootProject = ProjectDescriptor {
        name = "test"
    }
    + added by call: (top-level-object).enableFeaturePreview#13(name = "TYPESAFE_PROJECT_ACCESSORS")
    + added by call: (top-level-object).include#14(projectPath = ":app")
    + added by call: (top-level-object).include#15(projectPath = ":app-nia-catalog")
    + added by call: (top-level-object).include#16(projectPath = ":benchmarks")
    + added by call: (top-level-object).include#17(projectPath = ":core:common")
    + added by call: (top-level-object).include#18(projectPath = ":core:data")
    + added by call: (top-level-object).include#19(projectPath = ":core:data-test")
    + added by call: (top-level-object).include#20(projectPath = ":core:database")
    + added by call: (top-level-object).include#21(projectPath = ":core:datastore")
    + added by call: (top-level-object).include#22(projectPath = ":core:datastore-proto")
    + added by call: (top-level-object).include#23(projectPath = ":core:datastore-test")
    + added by call: (top-level-object).include#24(projectPath = ":core:designsystem")
    + added by call: (top-level-object).include#25(projectPath = ":core:domain")
    + added by call: (top-level-object).include#26(projectPath = ":core:model")
    + added by call: (top-level-object).include#27(projectPath = ":core:network")
    + added by call: (top-level-object).include#28(projectPath = ":core:ui")
    + added by call: (top-level-object).include#29(projectPath = ":core:testing")
    + added by call: (top-level-object).include#30(projectPath = ":core:analytics")
    + added by call: (top-level-object).include#31(projectPath = ":core:notifications")
    + added by call: (top-level-object).include#32(projectPath = ":feature:foryou")
    + added by call: (top-level-object).include#33(projectPath = ":feature:interests")
    + added by call: (top-level-object).include#34(projectPath = ":feature:bookmarks")
    + added by call: (top-level-object).include#35(projectPath = ":feature:topic")
    + added by call: (top-level-object).include#36(projectPath = ":feature:search")
    + added by call: (top-level-object).include#37(projectPath = ":feature:settings")
    + added by call: (top-level-object).include#38(projectPath = ":lint")
    + added by call: (top-level-object).include#39(projectPath = ":sync:work")
    + added by call: (top-level-object).include#40(projectPath = ":sync:sync-test")
    + added by call: (top-level-object).include#41(projectPath = ":ui-test-hilt-manifest")
}
```