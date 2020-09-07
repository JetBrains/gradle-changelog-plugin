# Gradle Changelog Plugin

[![official JetBrains project](https://jb.gg/badges/official.svg)][jb:confluence-on-gh]
[![Twitter Follow](https://img.shields.io/twitter/follow/JBPlatform?style=flat)][jb:twitter]
[![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]
[![Build](https://github.com/JetBrains/gradle-changelog-plugin/workflows/Build/badge.svg)][gh:build]
[![Slack](https://img.shields.io/badge/Slack-%23gradle--changelog--plugin-blue)][jb:slack]

**This project requires Gradle 5.4.1 or newer**

> **TIP:** Upgrade Gradle Wrapper with `./gradlew wrapper --gradle-version 6.6.1`

A Gradle plugin that provides tasks and helper methods to simplify working with a changelog that is managed
in the [keep a changelog][keep-a-changelog] style.

- [Usage](#usage)
- [Configuration](#configuration)
- [Tasks](#tasks)
    - [initializeChangelog](#initializechangelog)
    - [getChangelog](#getchangelog)
- [Methods](#methods)
    - [get](#get)
    - [getUnreleased](#getunreleased)
    - [getLatest](#getlatest)
    - [getAll](#getall)
    - [hasVersion](#hasversion)
- [Changelog.Item](#changelogitem)
- [Gradle Closure in Kotlin DSL](#gradle-closure-in-kotlin-dsl)
- [Helper Methods](#helper-methods)
- [Usage Examples](#usage-examples)

## Usage

Kotlin:
```kotlin
import org.jetbrains.changelog.closure

plugins {
    id("org.jetbrains.changelog") version "0.5.0"
}

tasks {
    // ...

    patchPluginXml {
        changeNotes(closure { changelog.getUnreleased().toHTML() })
    }
}

changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    header = closure { "[${project.version}]" }
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
}
```

Groovy:
```groovy
plugins {
    id 'org.jetbrains.changelog' version '0.5.0'
}

apply plugin: 'org.jetbrains.changelog'

intellij {
    // ...

    patchPluginXml {
        changeNotes({ changelog.getUnreleased().toHTML() })
    }
}

changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    header = { "[${project.version}]" }
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = ["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]
}
```

> **Note:** `closure` is a wrapper for the `KotlinClosure0` class and can be used directly as following:
> ```kotlin
> import org.gradle.kotlin.dsl.KotlinClosure0
> 
> changeNotes(KotlinClosure0({ changelog.getUnreleased() }))
> ```


## Configuration

Plugin can be configured with the following properties set in the `changelog {}` closure:

| Property                | Description                                                | Default value                                                        |
| ----------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------- |
| `groups`                | List of groups created with a new Unreleased section.      | `["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]` |
| `header`                | Closure that returns current header value.                 | `{ "[${project.version}]" }`                                         |
| `itemPrefix`            | Single item's prefix, allows to customise the bullet sign. | `"-"`                                                                |
| `keepUnreleasedSection` | Add Unreleased empty section after patching.               | `true`                                                               |
| `patchEmpty`            | Patches changelog even if no release note is provided.     | `true`                                                               |
| `path`                  | Path to the changelog file.                                | `"${project.projectDir}/CHANGELOG.md"`                               |
| `unreleasedTerm`        | Unreleased section text.                                   | `"[Unreleased]"`                                                     |
| `version`               | Current project's version.                                 | `"${project.version}"`                                               |


## Tasks

The plugin introduces the following tasks:

| Task                  | Description                                                          |
| --------------------- | -------------------------------------------------------------------- |
| `getChangelog`        | Retrieves changelog for the specified version.                       |
| `initializeChangelog` | Creates new changelog file with Unreleased section and empty groups. |
| `patchChangelog`      | Updates the unreleased section to the given version.                 |

### `initializeChangelog`

#### Examples

```bash
$ ./gradlew initializeChangelog
$ cat CHANGELOG.md

## [Unreleased]
### Added
- Example item

### Changed

### Deprecated

### Removed

### Fixed

### Security
```

### `getChangelog`

#### Options

| Option          | Description                                        |
| --------------- | -------------------------------------------------- |
| `--no-header`   | Skips the first version header line in the output. |
| `--unreleased`  | Returns Unreleased change notes.                   |

#### Examples

```bash
$ ./gradlew getChangelog --console=plain -q --no-header

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

The method returns a `Changelog.Item` object for the specified version.
Throws `MissingVersionException` if the version is not available.

It is possible to specify the *unreleased* section with setting the `${changelog.unreleasedTerm}` value.


#### Parameters

| Parameter   | Type      | Description             | Default value          |
| ----------- | --------- | ----------------------- | ---------------------- |
| `version`   | `String`  | Change note version.    | `${changelog.version}` |

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes(closure { changelog.get("1.0.0").toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes({ changelog.get("1.0.0").toHTML() })
    }
}
```

### `getUnreleased`

The method returns a `Changelog.Item` object for the *unreleased* version.

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes(closure { changelog.getUnreleased().toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes({ changelog.getUnreleased().toHTML() })
    }
}
```

### `getLatest`

The method returns the latest `Changelog.Item` object (first on the list).

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes(closure { changelog.getLatest().toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes({ changelog.getLatest().toHTML() })
    }
}
```

### `getAll`

The method returns all available `Changelog.Item` objects.

#### Examples

Kotlin:
```kotlin
extension.getAll().values.map { it.toText() }
```

Groovy:
```groovy
extension.getAll().values().each { it.toText() }
```

### `hasVersion`

The method checks if the giver version exists in the changelog

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        closure { changelog.hasVersion() }
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        { changelog.hasVersion() }
    }
}
```

## `Changelog.Item`

Methods described in the above section return `Changelog.Item` object, which is a representation of the single
changelog section for the specific version. It provides a couple of properties and methods that allow altering
the output form of the change notes:

### Properties 

| Name      | Type      | Description             |
| --------- | --------- | ----------------------- |
| `version` | `String`  | Change note version.    |

### Methods

| Name                | Description                    | Returned type |
| ------------------- | ------------------------------ | ------------- |
| `noHeader()`        | Excludes header part.          | `this`        |
| `noHeader(Boolean)` | Includes/excludes header part. | `this`        |
| `getHeader()`       | Returns header text.           | `String`      |
| `toText()`          | Generates Markdown output.     | `String`      |
| `toPlainText()`     | Generates Plain Text output.   | `String`      |
| `toString()`        | Generates Markdown output.     | `String`      |
| `toHTML()`          | Generates HTML output.         | `String`      |

## Gradle Closure in Kotlin DSL

To produce Gradle-specific closure in Kotlin DSL, required by some third-party plugins, like
[gradle-intellij-plugin][gh:gradle-intellij-plugin] it is required to wrap the Kotlin Unit with `KotlinClosure0` class:

```kotlin
KotlinClosure0({ changelog.get() })
```

There is also a *neater* method available:

```kotlin
import org.jetbrains.changelog.closure

closure { changelog.get() }
```

## Helper Methods

| Name                                   | Description                                                     | Returned type |
| -------------------------------------- | --------------------------------------------------------------- | ------------- |
| `closure(function: () -> T)`           | Produces Gradle-specific Closure for Kotlin DSL.                | `Closure<T>`  |
| `date(pattern: String = "yyyy-MM-dd")` | Shorthand for retrieving the current date in the given format.  | `String`      |
| `markdownToHTML(input: String)`        | Converts given Markdown content to HTML output.                 | `String`      |
| `markdownToPlainText(input: String)`   | Converts given Markdown content to Plain Text output.           | `String`      |


## Usage Examples

- [Unity Support for ReSharper and Rider](https://github.com/JetBrains/resharper-unity)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Package Search](https://plugins.jetbrains.com/plugin/12507-package-search)

[gh:build]: https://github.com/JetBrains/gradle-changelog-plugin/actions?query=workflow%3ABuild
[gh:gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin

[jb:confluence-on-gh]: https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub
[jb:slack]: https://plugins.jetbrains.com/slack
[jb:twitter]: https://twitter.com/JBPlatform

[build-phases]: https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases
[keep-a-changelog]: https://keepachangelog.com/en/1.0.0
[gradle-plugin-shield]: https://img.shields.io/maven-metadata/v.svg?label=Gradle%20Plugin&color=blue&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/intellij/plugins/gradle-changelog-plugin/maven-metadata.xml
[gradle-plugin]: https://plugins.gradle.org/plugin/org.jetbrains.changelog
