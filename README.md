[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Gradle Changelog Plugin

**This project requires Gradle 5.0 or newer**

A Gradle plugin that provides tasks and helper methods to let the working with the changelog managed
in a [keep a changelog][keep-a-changelog] style easier.

## Usage

Kotlin:
```kotlin
import org.jetbrains.changelog.closure

plugins {
    id("org.jetbrains.changelog") version "0.1.4"
}

tasks {
    // ...

    patchPluginXml {
        changeNotes(closure { changelog.getUnreleased(true) })
    }
}

changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    format = "[{0}]"
    keepUnreleasedSection = true
    unreleasedTerm = "Unreleased"
}
```

Groovy:
```groovy
plugins {
    id 'org.jetbrains.changelog' version '0.1.4'
}

apply plugin: 'org.jetbrains.changelog'

intellij {
    // ...

    patchPluginXml {
        changeNotes({ changelog.getUnreleased(true) })
    }
}

changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    format = "[{0}]"
    keepUnreleasedSection = true
    unreleasedTerm = "Unreleased"
}
```

> **Note:** `closure` is a wrapper for the `KotlinClosure0` class and can be used directly as following:
> ```kotlin
> import org.gradle.kotlin.dsl.KotlinClosure0
> 
> changeNotes(KotlinClosure0({ changelog.getUnreleased(true) }))
> ```


## Configuration

Plugin can be configured with the following properties set in the `changelog {}` closure:

| Property                | Description                                  | Default value                          |
| ----------------------- | -------------------------------------------- | -------------------------------------- |
| `version`               | Current project's version.                   | `"${project.version}"`                 |
| `path`                  | Path to the changelog file.                  | `"${project.projectDir}/CHANGELOG.md"` |
| `format`                | Format of the version section header.        | `"[{0}]"`                              |
| `keepUnreleasedSection` | Add Unreleased empty section after patching. | `true`                                 |
| `unreleasedTerm`        | Unreleased section text.                     | `"Unreleased"`                         |


## Tasks

The plugin introduces the following tasks:

| Task             | Description                                          |
| ---------------- | ---------------------------------------------------- |
| `getChangelog`   | Retrieves changelog for the specified version.       |
| `patchChangelog` | Updates the unreleased section to the given version. |

### `getChangelog`

#### Options

| Option          | Description                                       |
| --------------- | ------------------------------------------------- |
| `--no-header`   | Skips the first version header line in the output. |

#### Examples

```bash
$ ./gradlw getChangelog --no-daemon --console=plain -q --no-header

### Added
- Initial project scaffold
- GitHub Actions to automate testing and deployment
- Kotlin support
```


## Methods

All the methods are available via the `changelog` extension and allow for reading the changelog file within
the Gradle tasks to provide the latest (or specific) change notes.

> **Note:** Following methods depend on the `changelog` extension, which is set in the *Configuration*
> [build phase][build-phases]. To make it running properly, it's required to place the configuration before the fist
> usage of such a method. Alternatively, you can pass the Gradle closure to the task, which will postpone the method
> invocation.

### `get`

The method returns a change note for the specified version. Throws `MissingVersionException` if the version is not available.

It is possible to specify the *unreleased* section with setting the `${changelog.unreleasedTerm}` value.

#### Parameters

| Parameter   | Type      | Description             | Default value          |
| ----------- | --------- | ----------------------- | ---------------------- |
| `version`   | `String`  | Change note version.    | `${changelog.version}` |
| `asHTML`    | `Boolean` | Returns output as HTML. | `false`                |

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes(closure { changelog.get("1.0.0", true) })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes({ changelog.get("1.0.0", true) })
    }
}
```

### `getUnreleased`

Returns the *unreleased* change note.

#### Parameters

| Parameter   | Type      | Description             | Default value  |
| ----------- | --------- | ----------------------- | -------------- |
| `asHTML`    | `Boolean` | Returns output as HTML. | `false`        |

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes(closure { changelog.getLatest(true) })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes({ changelog.getLatest(true) })
    }
}
```


## Gradle closure in Kotlin DSL

To produce Gradle-specific closure in Kotlin DSL, required by some third-party plugins, like
[gradle-intellij-plugin][gradle-intellij-plugin] it is required to wrap the Kotlin Unit with `KotlinClosure0` class:

```kotlin
KotlinClosure0({ changelog.get() })
```

There is also a *neater* method available:

```kotlin
import org.jetbrains.changelog.closure

closure { changelog.get() }
```

[build-phases]: https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases
[keep-a-changelog]: https://keepachangelog.com/en/1.0.0
[gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin
