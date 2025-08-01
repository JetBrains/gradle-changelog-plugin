# Gradle Changelog Plugin

[![official JetBrains project](https://jb.gg/badges/official.svg)][jb:github]
[![X Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=x)][jb:x]
[![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]
[![Build](https://github.com/JetBrains/gradle-changelog-plugin/workflows/Build/badge.svg)][gh:build]
[![Slack](https://img.shields.io/badge/Slack-%23gradle--changelog--plugin-blue)][jb:slack]

A Gradle plugin providing tasks and helper methods to simplify working with a changelog that is managed in the [keep a changelog][keep-a-changelog] style.

> **Note**
>
> **This project requires Gradle 6.8 or newer**
>
> Upgrade Gradle Wrapper with `./gradlew wrapper --gradle-version 8.3`

## Table of contents

- [Usage](#usage)
- [Configuration](#configuration)
- [Tasks](#tasks)
    - [`getChangelog`](#getchangelog)
    - [`initializeChangelog`](#initializechangelog)
    - [`patchChangelog`](#patchchangelog)
- [Extension Methods](#extension-methods)
    - [`get`](#changeloggetversion-string-changelogitem)
    - [`getUnreleased`](#changeloggetunreleased-changelogitem)
    - [`getLatest`](#changeloggetlatest-changelogitem)
    - [`has`](#changeloghasversion-string-boolean)
    - [`render`](#changelogrenderoutputtype-changelogoutputtype-string)
    - [`renderItem`](#changelogrenderitemitem-changelogitem-outputtype-changelogoutputtype-string)
    - [`getInstance`](#changeloggetinstance-changelog)
- [Classes](#classes)
    - [`Changelog`](#changelog-class)
    - [`Changelog.OutputType`](#changelogoutputtype-enum)
    - [`Changelog.Item`](#changelogitem-class)
- [Helper Methods](#helper-methods)
- [Usage Examples](#usage-examples)
- [Changelog](#changelog)
- [Contributing](#contributing)
- [License](#license)

## Usage

The latest available version is: [![Gradle Plugin][gradle-plugin-shield]][gradle-plugin]

> **Note**
>
> The `patchPluginXml` task is defined in [Gradle IntelliJ Plugin][gh:gradle-intellij-plugin]

**build.gradle.kts** (Kotlin)

```kotlin
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogSectionUrlBuilder
import org.jetbrains.changelog.date

plugins {
    id("org.jetbrains.changelog") version "..."
}

tasks {
    // ...

    patchPluginXml {
        changeNotes = provider {
            changelog.renderItem(
                changelog
                    .getUnreleased()
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        }
    }
}

changelog {
    version = "1.0.0"
    path = file("CHANGELOG.md").canonicalPath
    header = provider { "[${version.get()}] - ${date()}" }
    headerParserRegex = """(\d+\.\d+)""".toRegex()
    introduction =
        """
        My awesome project that provides a lot of useful features, like:
        
        - Feature 1
        - Feature 2
        - and Feature 3
        """.trimIndent()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
    lineSeparator = "\n"
    combinePreReleases = true
    sectionUrlBuilder = ChangelogSectionUrlBuilder { repositoryUrl, currentVersion, previousVersion, isUnreleased -> "foo" }
    outputFile = file("release-note.txt")
}
```

**build.gradle** (Groovy)

```groovy
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogSectionUrlBuilder
import org.jetbrains.changelog.ExtensionsKt

plugins {
    id 'org.jetbrains.changelog' version '...'
}

apply plugin: 'org.jetbrains.changelog'

intellij {
    // ...

    patchPluginXml {
        changeNotes = provider {
            changelog.renderItem(
                changelog
                    .getUnreleased()
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        }
    }
}

changelog {
    version = "1.0.0"
    path = file("CHANGELOG.md").canonicalPath
    header = "[${-> version.get()}] - ${ExtensionsKt.date("yyyy-MM-dd")}"
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
    sectionUrlBuilder = { repositoryUrl, currentVersion, previousVersion, isUnreleased -> "foo" } as ChangelogSectionUrlBuilder
    outputFile = file("release-note.txt")
}
```

## Configuration

Plugin can be configured with the following properties set in the `changelog {}` closure:

| Property                | Description                                                                                                      |                                                                                                                                    |
|-------------------------|------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `versionPrefix`         | Version prefix used to compare tags.                                                                             | _Type:_ `String`                       <br/> _Default value:_ `v`                                                                  |
| `version`               | Current version. By default, project's version is used.                                                          | _Type:_ `String`                       <br/> _Default value:_ `project.version`                                                    |
| `path`                  | Path to the changelog file.                                                                                      | _Type:_ `String`                       <br/> _Default value:_ `file("CHANGELOG.md").cannonicalPath`                                |
| `preTitle`              | Optional content placed before the `title`.                                                                      | _Type:_ `String?`                      <br/> _Default value:_ `null`                                                               |
| `title`                 | The changelog title set as the top-lever header – `#`.                                                           | _Type:_ `String`                       <br/> _Default value:_ `"Changelog"`                                                        |
| `introduction`          | Optional content placed after the `title`.                                                                       | _Type:_ `String?`                      <br/> _Default value:_ `null`                                                               |
| `header`                | Header value used when patching the *Unreleased* section with text containing the current version.               | _Type:_ `String`                       <br/> _Default value:_ `provider { "${version.get()} - ${date()}" }`                        |
| `headerParserRegex`     | `Regex`/`Pattern`/`String` used to extract version from the header string.                                       | _Type:_ `Regex` / `Pattern` / `String` <br/> _Default value:_ `null`, fallbacks to [`SEM_VER_REGEX`][semver-regex]                 |
| `unreleasedTerm`        | Unreleased section name.                                                                                         | _Type:_ `String`                       <br/> _Default value:_ `"[Unreleased]"`                                                     |
| `keepUnreleasedSection` | Add an unreleased empty section on the top of the changelog after running the patching task.                     | _Type:_ `Boolean`                      <br/> _Default value:_ `true`                                                               |
| `patchEmpty`            | Patches changelog even if no release note is provided.                                                           | _Type:_ `Boolean`                      <br/> _Default value:_ `true`                                                               |
| `groups`                | List of groups created with a new Unreleased section.                                                            | _Type:_ `String`                       <br/> _Default value:_ `["Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"]` |
| `itemPrefix`            | Single item's prefix, allows to customise the bullet sign.                                                       | _Type:_ `String`                       <br/> _Default value:_ `"-"`                                                                |
| `combinePreReleases`    | Combines pre-releases (like `1.0.0-alpha`, `1.0.0-beta.2`) into the final release note when patching.            | _Type:_ `Boolean`                      <br/> _Default value:_ `true`                                                               |
| `lineSeparator`         | Line separator used for generating changelog content.                                                            | _Type:_ `String`                       <br/> _Default value:_ `"\n"` or determined from the existing file                          |
| `repositoryUrl`         | The GitHub repository URL used to build release links. If provided, leads to the GitHub comparison page.         | _Type:_ `String?`                      <br/> _Default value:_ `null`                                                               |
| `outputFile`            | Output file to write the changelog content to when using the `getChangelog` task.                                | _Type:_ `RegularFileProperty`                      <br/> _Default value:_ `null`                                                   |
| `sectionUrlBuilder`     | Function to build a single URL to link section with the GitHub page to present changes within the given release. | _Type:_ `ChangelogSectionUrlBuilder`   <br/> _Default value:_ Common `ChangelogSectionUrlBuilder` implementation                   |

> **Note**
>
> The `header` closure has the delegate explicitly set to the extension's context for the sake of the [Configuration cache][configuration-cache] support.

## Tasks

The plugin introduces the following tasks:

| Task                                          | Description                                                               |
|-----------------------------------------------|---------------------------------------------------------------------------|
| [`getChangelog`](#getchangelog)               | Retrieves changelog for the specified version.                            |
| [`initializeChangelog`](#initializechangelog) | Creates a new changelog file with an unreleased section and empty groups. |
| [`patchChangelog`](#patchchangelog)           | Updates the unreleased section to the given version.                      |

### `getChangelog`

Retrieves changelog for the specified version.

#### Options

| Option                | Type      | Default value | Description                                             |
|-----------------------|-----------|---------------|---------------------------------------------------------|
| `--no-header`         | `Boolean` | `false`       | Omits the section header in the changelog output.       |
| `--no-summary`        | `Boolean` | `false`       | Omits the section summary in the changelog output.      |
| `--no-links`          | `Boolean` | `false`       | Omits links in the changelog output.                    |
| `--no-empty-sections` | `Boolean` | `false`       | Omits empty sections in the changelog output.           |
| `--project-version`   | `String?` | `null`        | Returns change notes for the specified project version. |
| `--unreleased`        | `Boolean` | `false`       | Returns change notes for an unreleased section.         |
| `--output-file`       | `String?` | `null`        | File to write the changelog content to.                 |

#### Examples

```shell
$ ./gradlew getChangelog --console=plain -q --no-header --no-summary

### Added
- Initial project scaffold
- GitHub Actions to automate testing and deployment
- Kotlin support
```

```shell
$ ./gradlew getChangelog --output-file=build/changelog.md
$ cat build/changelog.md

## [1.0.0]

Initial release.

### Added
- Initial project scaffold
- GitHub Actions to automate testing and deployment
- Kotlin support

[1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/releases/tag/v1.0.0
```

### `initializeChangelog`

Creates a new changelog file with an unreleased section and empty groups.

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

#### Options

| Option           | Type      | Default value | Description                                            |
|------------------|-----------|---------------|--------------------------------------------------------|
| `--release-note` | `String?` | `null`        | Use custom release note to create new changelog entry. |

> **Warning**
>
> Content provided with the `--release-note` option will override the existing release note for the latest unreleased entry.

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

All the methods are available via the `changelog` extension and allow for reading the changelog file within the Gradle tasks to provide the latest (or specific) change notes.

> **Note**
>
> The following methods depend on the `changelog` extension set in the *Configuration* [build phase][build-phases].
> For safe access and process of your changelog file, we recommend accessing the `changelog` extension within Gradle closures as presented in the [Usage](#usage) section.

### `changelog.get(version: String): Changelog.Item`

The method returns a `Changelog.Item?` object for the specified version.
Throws `MissingVersionException` if the version is not available.

#### Parameters

| Parameter | Type     | Description          |
|-----------|----------|----------------------|
| `version` | `String` | Change note version. |

### `changelog.getUnreleased(): Changelog.Item`

The method returns a `Changelog.Item` object for the unreleased version.
Throws `MissingVersionException` if the version is not available.

### `changelog.getLatest(): Changelog.Item`

The method returns the latest released `Changelog.Item` object (first on the list).
Throws `MissingVersionException` if the version is not available.

### `changelog.has(version: String): Boolean`

The method checks if the given version exists in the changelog.

#### Parameters

| Parameter | Type     | Description          |
|-----------|----------|----------------------|
| `version` | `String` | Change note version. |

### `changelog.render(outputType: Changelog.OutputType): String`

Renders the whole `Changelog` object to string based on the given `outputType`.

#### Parameters

| Parameter    | Type                   | Description                                                         |
|--------------|------------------------|---------------------------------------------------------------------|
| `outputType` | `Changelog.OutputType` | Output type, see [Changelog.OutputType](#changelog-outputtype-enum) |

### `changelog.renderItem(item: Changelog.Item, outputType: Changelog.OutputType): String`

Renders the given `Changelog.Item` object to string based on the given `outputType`.

#### Parameters

| Parameter    | Type                   | Description                                                         |
|--------------|------------------------|---------------------------------------------------------------------|
| `item`       | `Changelog.Item`       | Item to render, see [Changelog.Item](#changelog-item-class)         |
| `outputType` | `Changelog.OutputType` | Output type, see [Changelog.OutputType](#changelog-outputtype-enum) |

### `changelog.getInstance(): Changelog`

Returns the `Changelog` instance shared among all the tasks.
See [`Changelog`](#changelog-class) for more details.

## Classes

### `Changelog` class

The `Changelog` class is a wrapper for the `Changelog` file.
It provides methods to read and write the changelog file.

#### Properties

| Name             | Type                          | Description                                                                                    |
|------------------|-------------------------------|------------------------------------------------------------------------------------------------|
| `preTitle`       | `String`                      | Optional content placed before the `title`.                                                    |
| `title`          | `String`                      | The changelog title set as the top-lever header – `#`.                                         |
| `introduction`   | `String`                      | Optional content placed after the `title`.                                                     |
| `items`          | `Map<String, Changelog.Item>` | List of all items available in the changelog stored in a map of `version` to `Changelog.Item`. |
| `unreleasedItem` | `Changelog.Item?`             | An instance of the unreleased item, may be `null`.                                             |
| `releasedItems`  | `List<Changelog.Item>`        | List of already released item instances.                                                       |
| `links`          | `Map<String, String>`         | List of all links stored at the end of the changelog in a map of `id` to `url`.                |

#### Methods

| Name                                                                 | Return type      | Description                                                                          |
|----------------------------------------------------------------------|------------------|--------------------------------------------------------------------------------------|
| `get(version: String)`                                               | `Changelog.Item` | Returns item for the given `version`. Throws `MissingVersionException` if missing.   |
| `getLatest()`                                                        | `Changelog.Item` | Returns the latest released item. Throws `MissingVersionException` if missing.       |
| `has(version: String)`                                               | `Boolean`        | Checks if item with the given `version` exists.                                      |
| `render(outputType: Changelog.OutputType)`                           | `String`         | Renders the whole `Changelog` object to string based on the given `outputType`.      |
| `renderItem(item: Changelog.Item, outputType: Changelog.OutputType)` | `String`         | Renders the given `Changelog.Item` object to string based on the given `outputType`. |

### `Changelog.OutputType` enum

| Name         | Description                                   |
|--------------|-----------------------------------------------|
| `MARKDOWN`   | Default, Markdown content format.             |
| `PLAIN_TEXT` | Plain text with no Markdown syntax generated. |
| `HTML`       | HTML content created out of Markdown version. |

### `Changelog.Item` class

Methods described in the above section return `Changelog.Item` object, which is a representation of the single changelog section for the specific version.

It provides a couple of properties and methods that allow altering the output form of the change notes:

#### Properties

| Name           | Type      | Description                                                                                |
|----------------|-----------|--------------------------------------------------------------------------------------------|
| `version`      | `String`  | Item version.                                                                              |
| `header`       | `String`  | Literal representation of the given item – may contain version with extra meta, like date. |
| `summary`      | `String`  | Optional summary of the release item.                                                      |
| `isUnreleased` | `Boolean` | Determines if an item is released or not.                                                  |

#### Methods

| Name                         | Description                                                                   | Returned type    |
|------------------------------|-------------------------------------------------------------------------------|------------------|
| `withHeader(Boolean)`        | Includes header part in the output.                                           | `Changelog.Item` |
| `withLinkedHeader(Boolean)`  | Adds link to the version in the header.                                       | `Changelog.Item` |
| `withSummary(Boolean)`       | Includes summary part.                                                        | `Changelog.Item` |
| `withLinks(Boolean)`         | Returns links used in the release section at the end.                         | `Changelog.Item` |
| `withEmptySections(Boolean)` | Prints empty sections.                                                        | `Changelog.Item` |
| `withFilter(Boolean)`        | Applies custom filter to the returned entries.                                | `Changelog.Item` |
| `toText()`                   | Deprecated. Use `changelog.renderItem(item)`                                  | `String`         |
| `toPlainText()`              | Deprecated. Use `changelog.renderItem(item, Changelog.OutputType.PLAIN_TEXT)` | `String`         |
| `toString()`                 | Deprecated. Use `changelog.renderItem(item, Changelog.OutputType.MARKDOWN)`   | `String`         |
| `toHTML()`                   | Deprecated. Use `changelog.renderItem(item, Changelog.OutputType.HTML)`       | `String`         |

## Helper Methods

| Name                                                          | Description                                                    | Returned type |
|---------------------------------------------------------------|----------------------------------------------------------------|---------------|
| `date(pattern: String = "yyyy-MM-dd")`                        | Shorthand for retrieving the current date in the given format. | `String`      |
| `markdownToHTML(input: String, lineSeparator: String = "\n")` | Converts given Markdown content to HTML output.                | `String`      |
| `markdownToPlainText(input: String, lineSeparator: String)`   | Converts given Markdown content to Plain Text output.          | `String`      |

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
- [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [Unity Support for ReSharper and Rider](https://github.com/JetBrains/resharper-unity)

## Changelog

All releases are available in the [Releases](https://github.com/JetBrains/gradle-changelog-plugin/releases) section.
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
[jb:x]: https://x.com/JBPlatform
[build-phases]: https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases
[configuration-cache]: https://docs.gradle.org/6.8.2/userguide/configuration_cache.html
[keep-a-changelog]: https://keepachangelog.com/en/1.0.0
[gradle-plugin-shield]: https://img.shields.io/maven-metadata/v.svg?label=Gradle%20Plugin&color=blue&metadataUrl=https://plugins.gradle.org/m2/org/jetbrains/intellij/plugins/gradle-changelog-plugin/maven-metadata.xml
[gradle-plugin]: https://plugins.gradle.org/plugin/org.jetbrains.changelog
[gradle-lazy-configuration]: https://docs.gradle.org/current/userguide/lazy_configuration.html
[semver-regex]: https://github.com/JetBrains/gradle-changelog-plugin/blob/main/src/main/kotlin/org/jetbrains/changelog/ChangelogPluginConstants.kt#L40
