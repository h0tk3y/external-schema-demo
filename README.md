### `external-schema-demo`

This is a demonstration of interpreting Restricted DSL files using a schema exported
from a Gradle build.

> ```shell
> git clone https://github.com/h0tk3y/external-schema-demo
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
    * `--mode={LOWLEVEL|DOM}` (the default is `LOWLEVEL`) to specify which view of the data to use (see [Data Views](#data-views))
   
3. The resulting output is the content of the top-level-receiver. 
   Starting from the top level, each object has its properties and _added objects_ printed recursively.

### Data Views

The demo supports two kinds of data representation. You can choose one of them by using the `--mode=...` option.

#### `DOM` data representation

This representation accesses the declarative file via a high-level API. It can print either a "raw" document that 
has not been validated against any schema (for this, do not pass `--schema=...`) or a resolved document, which has all of its content validated against the schema.

Passing `--locations=true` in the DOM view will also add the source indices to the content nodes in the output. 

Here's an example output of a resolved document (the resolution results are the labels like `✓:Point` or `+:Access`):
```text
element("restricted", configure:Extension) {
    property("id", ✓:String, literal("test"))
    property("referencePoint", ✓:Point, valueFactory("point", ✓:Point, literal(1), literal(2)))
    element("primaryAccess", configure:Access) {
        property("read", ✓:Boolean, literal(false))
        property("write", ✓:Boolean, literal(false))
    }
    element("secondaryAccess", +:Access) {
        property("name", ✓:String, literal("two"))
        property("read", ✓:Boolean, literal(true))
        property("write", ✓:Boolean, literal(false))
    }
    element("secondaryAccess", +:Access) {
        property("name", ✓:String, literal("three"))
        property("read", ✓:Boolean, literal(true))
        property("write", ✓:Boolean, literal(true))
    }
}
```


#### `LOWLEVEL` data representation

This form is the view on the data that Gradle is using under the hood before mapping the declarative file content 
to the JVM objects.

An example output of this view:

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
}
```

### Demo project

Clone the `gradle/restricted-dsl-demo` branch of https://github.com/gradle/nowinandroid:

```shell
git clone --branch gradle/restricted-dsl-demo https://github.com/gradle/nowinandroid
cd nowinandroid
```

Run a Gradle build in that repository:

```shell
./gradlew :projects
```

It will produce the schema files:
* `.gradle/restricted-schema/settings.somethings.schema` for `settings.gradle.something`
* `feature/search/.gradle/restricted-schema/plugins.somethings.schema` for `feature/search/build.gradle.something`
* `feature/search/.gradle/restricted-schema/project.somethings.schema` for `feature/search/build.gradle.something`

After that, run the demo app, for example:

* To produce a raw (unvalidated) DOM of a project file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo\
        --script ../nowinandroid/feature/search/build.gradle.something \
        --mode DOM
    ```

* To produce a resolved DOM view of the settings file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/settings.gradle.something \
        --schema ../nowinandroid/.gradle/restricted-schema/settings.something.schema \
        --mode DOM
    ```

* To produce a resolved DOM view of a project file with source locations:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/feature/search/build.gradle.something \
        --schema ../nowinandroid/feature/search/.gradle/restricted-schema/project.something.schema \
        --mode DOM \
        --locations true
    ```

* To produce a low-level view of a project file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/feature/search/build.gradle.something \
        --schema ../nowinandroid/feature/search/.gradle/restricted-schema/project.something.schema \
        --mode LOWLEVEL
    ```