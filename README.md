# Gradle Changelog Plugin

[![official JetBrains project](https://jb.gg/badges/official.svg)][jb:confluence-on-gh]
[![Twitter Follow](https://img.shields.io/twitter/follow/JBPlatform?style=flat)][jb:twitter]
[![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]
[![Build](https://github.com/JetBrains/gradle-changelog-plugin/workflows/Build/badge.svg)][gh:build]
[![Slack](https://img.shields.io/badge/Slack-%23gradle--changelog--plugin-blue)][jb:slack]

**This project requires Gradle 6.6 or newer**

> **TIP:** Upgrade Gradle Wrapper with `./gradlew wrapper --gradle-version 7.2`

A Gradle plugin that provides tasks and helper methods to simplify working with a changelog that is managed in the [keep a changelog][keep-a-changelog] style.

## Table of contents

- [Usage](#usage)
- [Configuration](#configuration)
- [Tasks](#tasks)
    - [`initializeChangelog`](#initializechangelog)
    - [`getChangelog`](#getchangelog)
- [Extension Methods](#extension-methods)
    - [`get`](#get)
    - [`getOrNull`](#getOrNull)
    - [`getUnreleased`](#getunreleased)
    - [`getLatest`](#getlatest)
    - [`getAll`](#getall)
    - [`has`](#has)
- [`Changelog.Item`](#changelogitem)
- [Helper Methods](#helper-methods)
- [Usage Examples](#usage-examples)
- [Contributing](#contributing)


## Usage

Kotlin:
```kotlin
import org.jetbrains.changelog.date

plugins {
    id("org.jetbrains.changelog") version "1.2.1"
}

tasks {
    // ...

    patchPluginXml {
        changeNotes(provider { changelog.getUnreleased().toHTML() })
    }
}

changelog {
    version.set("1.0.0")
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}] - ${date()}" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}
```

Groovy:
```groovy
import org.jetbrains.changelog.ExtensionsKt

plugins {
    id 'org.jetbrains.changelog' version '1.2.1'
}

apply plugin: 'org.jetbrains.changelog'

intellij {
    // ...

    patchPluginXml {
        changeNotes({ changelog.getUnreleased().toHTML() })
    }
}

changelog {
    version = "1.0.0"
    path = "${project.projectDir}/CHANGELOG.md"
    header = "[${-> version.get()}] - ${new SimpleDateFormat("yyyy-MM-dd").format(new Date())}"
    headerParserRegex = ~/\d+\.\d+/
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = ["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]
}
```

> **Note:** All the extension and tasks properties are now lazy (see [Lazy Configuration][gradle-lazy-configuration]).
> 
> To set values in Kotlin DSL, use `.set(...)` method explicitly, like `changelog.version.set("1.0.0")`, in Groovy it is still possible to use `=` assignment.
>
> To access property value, call `.get()`, like: `changelog.version.get()` in both variants.


## Configuration

Plugin can be configured with the following properties set in the `changelog {}` closure:

| Property                | Description                                                                | Default value                                                        |
| ----------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **`version`**           | **Required.** Current project's version.                                   |                                                                      |
| `groups`                | List of groups created with a new Unreleased section.                      | `["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]` |
| `header`                | `String` or `Provider` that returns current header value.                  | `provider { "[${version.get()}]" }`                                  |
| `headerParserRegex`     | `Regex`/`Pattern`/`String` used to extract version from the header string. | `null`, fallbacks to [`ChangelogPluginConstants#SEM_VER_REGEX`][semver-regex]         |
| `itemPrefix`            | Single item's prefix, allows to customise the bullet sign.                 | `"-"`                                                                |
| `keepUnreleasedSection` | Add Unreleased empty section after patching.                               | `true`                                                               |
| `patchEmpty`            | Patches changelog even if no release note is provided.                     | `true`                                                               |
| `path`                  | Path to the changelog file.                                                | `"${project.projectDir}/CHANGELOG.md"`                               |
| `unreleasedTerm`        | Unreleased section text.                                                   | `"[Unreleased]"`                                                     |

> **Note:** `header` closure has the delegate explicitly set to the extension's context for the sake of the [Configuration cache][configuration-cache] support.

## Tasks

The plugin introduces the following tasks:

| Task                  | Description                                                                                                             |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `getChangelog`        | Retrieves changelog for the specified version.                                                                          |
| `initializeChangelog` | Creates new changelog file with Unreleased section and empty groups.                                                    |
| `patchChangelog`      | Updates the unreleased section to the given version. Requires *unreleased* section to be present in the changelog file. |

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

### `patchChangelog`

#### Options

| Option           | Description                                             |
| ---------------- | ------------------------------------------------------- |
| `--release-note` | Adds custom release note to the latest changelog entry. |

#### Examples

```bash
$ ./gradlew patchChangelog --release-note=- Foo
$ cat CHANGELOG.md

## [Unreleased]

## [1.0.0]
- Foo
```


## Extension Methods

All the methods are available via the `changelog` extension and allow for reading the changelog file within the Gradle tasks to provide the latest (or specific) change notes.

> **Note:** Following methods depend on the `changelog` extension, which is set in the *Configuration* [build phase][build-phases].
> To make it run properly, it's required to place the configuration before the fist usage of such a method.
> Alternatively, you can pass the Gradle closure to the task, which will postpone the method invocation.

### `get`

The method returns a `Changelog.Item` object for the specified version.
Throws `MissingVersionException` if the version is not available.

It is possible to specify the *unreleased* section with setting the `${changelog.unreleasedTerm}` value.

#### Parameters

| Parameter   | Type      | Description          | Default value          |
| ----------- | --------- | -------------------- | ---------------------- |
| `version`   | `String`  | Change note version. | `${changelog.version}` |

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes.set(provider { changelog.get("1.0.0").toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes = { changelog.get("1.0.0").toHTML() }
    }
}
```

### `getOrNull`

Same as `get`, but returns `null` instead of throwing `MissingVersionException`.

#### Parameters

| Parameter   | Type      | Description          | Default value          |
| ----------- | --------- | -------------------- | ---------------------- |
| `version`   | `String`  | Change note version. | `${changelog.version}` |

### `getUnreleased`

The method returns a `Changelog.Item` object for the *unreleased* version.

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        changeNotes.set(provider { changelog.getUnreleased().toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes = { changelog.getUnreleased().toHTML() }
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
        changeNotes.set(provider { changelog.getLatest().toHTML() })
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        changeNotes = { changelog.getLatest().toHTML() }
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

### `has`

The method checks if the given version exists in the changelog.

#### Examples

Kotlin:
```kotlin
tasks {
    patchPluginXml {
        provider { changelog.has("1.0.0") }
    }
}
```

Groovy:
```groovy
tasks {
    patchPluginXml {
        { changelog.has("1.0.0") }
    }
}
```

## `Changelog.Item`

Methods described in the above section return `Changelog.Item` object, which is a representation of the single changelog section for the specific version.

It provides a couple of properties and methods that allow altering the output form of the change notes:

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

## Helper Methods

| Name                                   | Description                                                    | Returned type |
| -------------------------------------- | -------------------------------------------------------------- | ------------- |
| `date(pattern: String = "yyyy-MM-dd")` | Shorthand for retrieving the current date in the given format. | `String`      |
| `markdownToHTML(input: String)`        | Converts given Markdown content to HTML output.                | `String`      |
| `markdownToPlainText(input: String)`   | Converts given Markdown content to Plain Text output.          | `String`      |

> **Note:** To use package-level Kotlin functions in Groovy, you need to import the containing file as a class:
> ```groovy
> import org.jetbrains.changelog.ExtensionsKt
> 
> changelog {
>   header = { "[$version] - ${ExtensionsKt.date('yyyy-MM-dd')}" }
> }
> ```

## Usage Examples

- [Unity Support for ReSharper and Rider](https://github.com/JetBrains/resharper-unity)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Package Search](https://plugins.jetbrains.com/plugin/12507-package-search)

## Contributing

### Integration tests

To perform integration tests with an existing project, bind the `gradle-changelog-plugin` sources in the Gradle settings file:

`settings.gradle`:
```
rootProject.name = "IntelliJ Platform Plugin Template"

includeBuild '/Users/hsz/Projects/JetBrains/gradle-changelog-plugin'
```

`settings.gradle.kts`:
```
rootProject.name = "IntelliJ Platform Plugin Template"

includeBuild("/Users/hsz/Projects/JetBrains/gradle-changelog-plugin")
```

[gh:build]: https://github.com/JetBrains/gradle-changelog-plugin/actions?query=workflow%3ABuild
[gh:gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin

[jb:confluence-on-gh]: https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub
[jb:slack]: https://plugins.jetbrains.com/slack
[jb:twitter]: https://twitter.com/JBPlatform

[build-phases]: https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases
[configuration-cache]: https://docs.gradle.org/6.8.2/userguide/configuration_cache.html
[keep-a-changelog]: https://keepachangelog.com/en/1.0.0
[gradle-plugin-shield]: https://img.shields.io/maven-metadata/v.svg?label=Gradle%20Plugin&color=blue&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/intellij/plugins/gradle-changelog-plugin/maven-metadata.xml
[gradle-plugin]: https://plugins.gradle.org/plugin/org.jetbrains.changelog
[gradle-lazy-configuration]: https://docs.gradle.org/current/userguide/lazy_configuration.html
[semver-regex]: https://github.com/JetBrains/gradle-changelog-plugin/blob/main/src/main/kotlin/org/jetbrains/changelog/ChangelogPluginConstants.kt#L38
