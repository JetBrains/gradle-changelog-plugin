# Gradle Changelog Plugin

[![official JetBrains project](https://jb.gg/badges/official.svg)][jb:github]
[![Twitter Follow](https://img.shields.io/twitter/follow/JBPlatform?style=flat)][jb:twitter]
[![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]
[![Build](https://github.com/JetBrains/gradle-changelog-plugin/workflows/Build/badge.svg)][gh:build]
[![Slack](https://img.shields.io/badge/Slack-%23gradle--changelog--plugin-blue)][jb:slack]

**This project requires Gradle 6.8 or newer**

> **Note**
>
> Upgrade Gradle Wrapper with `./gradlew wrapper --gradle-version 7.5.1`

A Gradle plugin providing tasks and helper methods to simplify working with a changelog that is managed in the [keep a changelog][keep-a-changelog] style.

## Table of contents

- [Usage](#usage)
- [Configuration](#configuration)
- [Tasks](#tasks)
    - [`initializeChangelog`](#initializechangelog)
    - [`getChangelog`](#getchangelog)
    - [`patchChangelog`](#patchchangelog)
- [Extension Methods](#extension-methods)
    - [`get`](#get)
    - [`getOrNull`](#getornull)
    - [`getUnreleased`](#getunreleased)
    - [`getLatest`](#getlatest)
    - [`has`](#has)
- [Extension Fields](#extension-fields)
    - [`instance`](#instance)
- [Classes](#classes)
    - [`Changelog`](#changelog-object)
    - [`Changelog.Item`](#changelogitem-object)
- [Helper Methods](#helper-methods)
- [Usage Examples](#usage-examples)
- [Contributing](#contributing)

## Usage

The latest available version is: [![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]

> **Note**
>
> The `patchPluginXml` task is defined in [Gradle IntelliJ Plugin][gh:gradle-intellij-plugin]

**build.gradle.kts** (Kotlin)

```kotlin
import org.jetbrains.changelog.date

plugins {
    id("org.jetbrains.changelog") version "..."
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
    headerParserRegex.set("""(\d+\.\d+)""".toRegex())
    introduction.set(
        """
        My awesome project that provides a lot of useful features, like:
        
        - Feature 1
        - Feature 2
        - and Feature 3
        """.trimIndent()
    )
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
    lineSeparator.set("\n")
    combinePreReleases.set(true)
}
```

**build.gradle** (Groovy)

```groovy
import java.text.SimpleDateFormat
import org.jetbrains.changelog.ExtensionsKt

plugins {
    id 'org.jetbrains.changelog' version '...'
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
    headerParserRegex = ~/(\d+\.\d+)/
    introduction = """
        My awesome project that provides a lot of useful features, like:
        
        - Feature 1
        - Feature 2
        - and Feature 3
    """.stripIndent()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = ["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]
    lineSeparator = "\n"
    combinePreReleases = true
}
```

> **Note**
>
> All the extension and tasks properties are now lazy (see [Lazy Configuration][gradle-lazy-configuration]).
>
> To set values in Kotlin DSL, use `.set(...)` method explicitly, like `changelog.version.set("1.0.0")`, in Groovy it is still possible to use `=` assignment.
>
> To access property value, call `.get()`, like: `changelog.version.get()` in both variants.

## Configuration

Plugin can be configured with the following properties set in the `changelog {}` closure:

| Property                | Description                                                                     | Default value                                                                 |
|-------------------------|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| **`version`**           | Current version. By default, global project's version is used.                  | `project.version`                                                             |
| `groups`                | List of groups created with a new Unreleased section.                           | `["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]`          |
| `preHeader`             | `String` or `Provider` that returns content placed before the changelog header. | `null`                                                                        |
| `header`                | `String` or `Provider` that returns current header value.                       | `provider { "[${version.get()}]" }`                                           |
| `headerParserRegex`     | `Regex`/`Pattern`/`String` used to extract version from the header string.      | `null`, fallbacks to [`ChangelogPluginConstants#SEM_VER_REGEX`][semver-regex] |
| `introduction`          | An optional portion of text that appears after the main header.                 | `null`                                                                        |
| `itemPrefix`            | Single item's prefix, allows to customise the bullet sign.                      | `"-"`                                                                         |
| `keepUnreleasedSection` | Add Unreleased empty section after patching.                                    | `true`                                                                        |
| `patchEmpty`            | Patches changelog even if no release note is provided.                          | `true`                                                                        |
| `path`                  | Path to the changelog file.                                                     | `"${project.projectDir}/CHANGELOG.md"`                                        |
| `unreleasedTerm`        | Unreleased section text.                                                        | `"[Unreleased]"`                                                              |
| `lineSeparator`         | Line separator used for generating changelog content.                           | `"\n"` or determined from the existing file                                   |
| `combinePreReleases`    | Combines pre-releases into the final release note when patching.                | `true`                                                                        |

> **Note**
>
> `header` closure has the delegate explicitly set to the extension's context for the sake of the [Configuration cache][configuration-cache] support.

## Tasks

The plugin introduces the following tasks:

| Task                                          | Description                                                                                                             |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| [`getChangelog`](#getchangelog)               | Retrieves changelog for the specified version.                                                                          |
| [`initializeChangelog`](#initializechangelog) | Creates new changelog file with Unreleased section and empty groups.                                                    |
| [`patchChangelog`](#patchchangelog)           | Updates the unreleased section to the given version. Requires *unreleased* section to be present in the changelog file. |

### `getChangelog`

Retrieves changelog for the specified version.

#### Options

| Option         | Type      | Description                                        |
|----------------|-----------|----------------------------------------------------|
| `--no-header`  | `Boolean` | Skips the first version header line in the output. |
| `--no-summary` | `Boolean` | Skips the summary section in the output.           |
| `--unreleased` | `Boolean` | Returns Unreleased change notes.                   |
| `--version`    | `String`  | Returns change notes for the specified version.    |

#### Examples

```shell
$ ./gradlew getChangelog --console=plain -q --no-header --no-summary

### Added
- Initial project scaffold
- GitHub Actions to automate testing and deployment
- Kotlin support
```

### `initializeChangelog`

Creates new changelog file with Unreleased section and empty groups.

#### Examples

```shell
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

### `patchChangelog`

Updates the unreleased section to the given version.
Requires *unreleased* section to be present in the changelog file.

#### Options

| Option           | Description                                             |
|------------------|---------------------------------------------------------|
| `--release-note` | Adds custom release note to the latest changelog entry. |

> **Warning**
> 
> Content provided with the `--release-note` option will override the existing release note for the latest "unreleased" entry.

#### Examples

```shell
$ cat CHANGELOG.md
## [Unreleased]
### Added
- A based new feature
 
$ ./gradlew patchChangelog
$ cat CHANGELOG.md

## [Unreleased]
### Added

## [1.0.0]
### Added
- A based new feature
```

```shell
$ cat CHANGELOG.md
## [Unreleased]
### Added
- This note will get overridden by the `--release-note`

$ ./gradlew patchChangelog --release-note='- Foo'
$ cat CHANGELOG.md
## [Unreleased]
### Added

## [1.0.0]
- Foo
```

## Extension Methods

All the methods are available via the `changelog` extension and allow for reading the changelog file within the Gradle tasks to provide the latest (or specific)
change notes.

> **Note**
>
> Following methods depend on the `changelog` extension, which is set in the *Configuration* [build phase][build-phases].
> To make it run properly, it's required to place the configuration before the first usage of such a method.
> Alternatively, you can pass the Gradle closure to the task, which will postpone the method invocation.

### `get`

The method returns a `Changelog.Item` object for the specified version.
Throws `MissingVersionException` if the version is not available.

It is possible to specify the *unreleased* section with setting the `${changelog.unreleasedTerm}` value.

#### Parameters

| Parameter | Type     | Description          | Default value          |
|-----------|----------|----------------------|------------------------|
| `version` | `String` | Change note version. | `${changelog.version}` |

#### Examples

**build.gradle.kts** (Kotlin)

```kotlin
tasks {
    patchPluginXml {
        changeNotes.set(provider { changelog.get("1.0.0").toHTML() })
    }
}
```

**build.gradle** (Groovy)

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

| Parameter | Type     | Description          | Default value          |
|-----------|----------|----------------------|------------------------|
| `version` | `String` | Change note version. | `${changelog.version}` |

### `getUnreleased`

The method returns a `Changelog.Item` object for the *unreleased* version.

#### Examples

**build.gradle.kts** (Kotlin)

```kotlin
tasks {
    patchPluginXml {
        changeNotes.set(provider { changelog.getUnreleased().toHTML() })
    }
}
```

**build.gradle** (Groovy)

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

**build.gradle.kts** (Kotlin)

```kotlin
tasks {
    patchPluginXml {
        changeNotes.set(provider { changelog.getLatest().toHTML() })
    }
}
```

**build.gradle** (Groovy)

```groovy
tasks {
    patchPluginXml {
        changeNotes = { changelog.getLatest().toHTML() }
    }
}
```

### `has`

The method checks if the given version exists in the changelog.

#### Examples

**build.gradle.kts** (Kotlin)

```kotlin
tasks {
    patchPluginXml {
        provider { changelog.has("1.0.0") }
    }
}
```

**build.gradle** (Groovy)

```groovy
tasks {
    patchPluginXml {
        {
            changelog.has("1.0.0")
        }
    }
}
```

## Extension Fields

All the fields available via the `changelog` extension and allow for the direct access to the `changelog` extension.

### `instance`

The field returns the current `Changelog` instance.

## Classes

### `Changalog` class

The `Changelog` class is a wrapper for the `Changelog` file.
It provides methods to read and write the changelog file.

#### Properties

| Name           | Type     | Description                                                                             |
|----------------|----------|-----------------------------------------------------------------------------------------|
| `preHeader`    | `String` | Section that appears before the actual changelog header.                                |
| `header`       | `String` | Changelog header.                                                                       |
| `introduction` | `String` | Static leading text introduction placed after the header and before changelog sections. |

#### Methods

| Name                   | Description                     | Returned type                 |
|------------------------|---------------------------------|-------------------------------|
| `has(String)`          | Checks if the version exists.   | `Boolean`                     |
| `get(version: String)` | Returns the change note object. | `Changelog.Item`              |
| `getLatest()`          | Returns the latest change note. | `Changelog.Item`              |

### `Changelog.Item` class

Methods described in the above section return `Changelog.Item` object, which is a representation of the single changelog section for the specific version.

It provides a couple of properties and methods that allow altering the output form of the change notes:

#### Properties

| Name      | Type     | Description          |
|-----------|----------|----------------------|
| `version` | `String` | Change note version. |

#### Methods

| Name                         | Description                       | Returned type |
|------------------------------|-----------------------------------|---------------|
| `withHeader(Boolean)`        | Includes/excludes header part.    | `this`        |
| `getHeader()`                | Returns header text.              | `String`      |
| `withSummary(Boolean)`       | Includes/excludes summary part.   | `this`        |
| `getSummary()`               | Returns summary text.             | `String`      |
| `withEmptySections(Boolean)` | Includes/excludes empty sections. | `this`        |
| `toText()`                   | Generates Markdown output.        | `String`      |
| `toPlainText()`              | Generates Plain Text output.      | `String`      |
| `toString()`                 | Generates Markdown output.        | `String`      |
| `toHTML()`                   | Generates HTML output.            | `String`      |

## Helper Methods

| Name                                   | Description                                                    | Returned type |
|----------------------------------------|----------------------------------------------------------------|---------------|
| `date(pattern: String = "yyyy-MM-dd")` | Shorthand for retrieving the current date in the given format. | `String`      |
| `markdownToHTML(input: String)`        | Converts given Markdown content to HTML output.                | `String`      |
| `markdownToPlainText(input: String)`   | Converts given Markdown content to Plain Text output.          | `String`      |

> **Note**
>
> To use package-level Kotlin functions in Groovy, you need to import the containing file as a class:
>
> ```groovy
> import org.jetbrains.changelog.ExtensionsKt
> 
> changelog {
>   header = { "[$version] - ${ExtensionsKt.date('yyyy-MM-dd')}" }
> }
> ```

## Usage Examples

- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Unity Support for ReSharper and Rider](https://github.com/JetBrains/resharper-unity)

## Changelog

All releases are available in the [Releases](https://github.com/JetBrains/gradle-intellij-plugin/releases) section.
The latest available version is:

[![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]

## Contributing

Please see [CONTRIBUTING](./CONTRIBUTING.md) on how to submit feedback and contribute to this project.

## License

Licensed under the Apache License, Version 2.0 (the "License"), see [LICENCE](./LICENSE).


[gh:build]: https://github.com/JetBrains/gradle-changelog-plugin/actions?query=workflow%3ABuild

[gh:gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md

[jb:slack]: https://plugins.jetbrains.com/slack

[jb:twitter]: https://twitter.com/JBPlatform

[build-phases]: https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases

[configuration-cache]: https://docs.gradle.org/6.8.2/userguide/configuration_cache.html

[keep-a-changelog]: https://keepachangelog.com/en/1.0.0

[gradle-plugin-shield]: https://img.shields.io/maven-metadata/v.svg?label=Gradle%20Plugin&color=blue&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/intellij/plugins/gradle-changelog-plugin/maven-metadata.xml

[gradle-plugin]: https://plugins.gradle.org/plugin/org.jetbrains.changelog

[gradle-lazy-configuration]: https://docs.gradle.org/current/userguide/lazy_configuration.html

[semver-regex]: https://github.com/JetBrains/gradle-changelog-plugin/blob/main/src/main/kotlin/org/jetbrains/changelog/ChangelogPluginConstants.kt#L38
