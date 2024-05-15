### `external-schema-demo`

This is a demonstration of interpreting Declarative DSL files using a schema exported
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
1. Run a Gradle build that exports declarative DSL schemas for settings and projects

    The Declarative DSL schemas may be found under `.gradle/declarative-schema/*.dcl.schema` in the root 
    project directory and in the subprojects.

    Note that schemas are only exported if a settings file or a project build file uses
    the Declarative DSL (`*.gradle.dcl`).

2. Run this demo application with the following command line options:

    * `--script <file>` pointing to a `settings.gradle.dcl` or `build.gradle.dcl` file;
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
* For the `settings.gradle.dcl` file:
  * `.gradle/declarative-schema/settingsPluginManagement.dcl.schema` – for evaluating `pluginManagement { ... }` 
  * `.gradle/declarative-schema/settingsPlugins.dcl.schema` for evaluating `plugins { ... }` in the settings file
  * `.gradle/declarative-schema/settings.dcl.schema` for the rest of the settings file content
* For a build file, e.g. `feature/search/build.gradle.dcl`:
  * `feature/search/.gradle/declarative-schema/plugins.dcl.schema` for the `plugins { ... }` block;
  * `feature/search/.gradle/declarative-schema/project.dcl.schema` for the rest of the build file.

After that, run the demo app, for example:

* To produce a raw (unvalidated) DOM of a project file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo\
        --script ../nowinandroid/feature/search/build.gradle.dcl \
        --mode DOM
    ```

* To produce a resolved DOM view of the settings file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/settings.gradle.dcl \
        --schema ../nowinandroid/.gradle/declarative-schema/settings.dcl.schema \
        --mode DOM
    ```

* To produce a resolved DOM view of a project file with source locations:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/feature/search/build.gradle.dcl \
        --schema ../nowinandroid/feature/search/.gradle/declarative-schema/project.dcl.schema \
        --mode DOM \
        --locations true
    ```

* To produce a low-level view of a project file:
    ```shell
    ./build/install/external-schema-demo/bin/external-schema-demo \
        --script ../nowinandroid/feature/search/build.gradle.dcl \
        --schema ../nowinandroid/feature/search/.gradle/declarative-schema/project.dcl.schema \
        --mode LOWLEVEL
    ```