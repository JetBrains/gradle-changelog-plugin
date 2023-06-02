# Gradle Changelog Plugin

## [Unreleased]

## [2.1.0] - 2023-06-02

### Added
- `versionPrefix` to allow setting the version prefix to compare tags [#139](../../issues/139)
- `--no-empty-sections` flag to `getChangelog` task [#167](../../issues/167)

### Fixed
- No-longer discard all but the last paragraph in list items [#133](../../issues/133) [#147](../../issues/147)

## [2.0.0] - 2022-10-28

### Added
- Allow for customizing the changelog introduction
- Make `changelog.instance` property public
- Introduce changelog `summary` and changelog property [#127](../../issues/127)
- Introduce changelog `preTitle` and `title` changelog properties
- Ensure patched changelog ends with a newline [#126](../../issues/126)
- Added the `changelog.lineSeparator` property to allow for customizing the line separator used in the changelog [#104](../../issues/104)
- Added the `--version=...` CLI parameter for the `getChangelog` task [#83](../../issues/83)
- Throw an exception when `initializeChangelog` task works on non-empty file [#82](../../issues/82)
- Remove empty sections from the changelog while patching [#28](../../issues/28)
- Added the `changelog.combinePreReleases` property to allow for combining pre-releases into a single section [#50](../../issues/50)

### Changed
- Upgrade minimal required Gradle version to `6.8`
- Make `withHeader` property of the `Changelog.Item` object `true` by default
- Updated the `HeaderParseException` message [#84](../../issues/84)
- Use `"[${project.version}] - ${date()}"` as the default value for the `changelog.header` property [#18](../../issues/18)

### Deprecated
- `Changelog.Item.toText()` replaced with `Changelog.renderItem(Chagnelog.Item)`
- `Changelog.Item.toHTML()` replaced with `Changelog.renderItem(Chagnelog.Item, Changelog.OutputType.HTML)`
- `Changelog.Item.toPlainText()` replaced with `Changelog.renderItem(Chagnelog.Item, Changelog.OutputType.PLAIN_TEXT)`

## [1.3.1] - 2021-10-13

### Changed
- Use the actual version numbers as keys for `extension.items` instead of raw header value

## [1.3.0] - 2021-08-25

### Added
- PatchChangelogTask: `--release-note` CLI option to add a custom release note to the latest changelog entry

## [1.2.1] - 2021-07-22

### Added
- `getOrNull` extension method
- Configuration Cache support

## [1.2.0] - 2021-07-05

### Added
- Task Configuration Avoidance
- Lazy Properties

### Fixed
- InitializeChangelogTask issue for no groups present

## [1.1.2] - 2021-02-15

### Changed
- Remove `shadowJar`

### Fixed
- Don't create groups for the new Unreleased section if empty array is provided

## [1.1.1] - 2021-02-09

### Changed
- Require `changelog.version` to be provided explicitly

### Fixed
- `unspecified` version when patching the changelog

## [1.0.1] - 2021-01-14

### Fixed
- Provide `project.version` to the extension using conventions

## [1.0.0] - 2021-01-12

### Added
- Support for the [Configuration cache](https://docs.gradle.org/6.8.1/userguide/configuration_cache.html)

### Changed
- `header` closure has the delegate explicitly set to the extension's context
- Upgrade Gradle Wrapper to `6.6`
- Upgrade `org.jetbrains.kotlin.jvm` to `1.4.21`
- Upgrade `io.gitlab.arturbosch.detekt` to `1.15.0`
- Upgrade `com.github.johnrengelman.shadow` to `6.1.0`

## [0.6.2] - 2020-10-13

### Changed
- Smart processing of `headerParserRegex` property

## [0.6.1] - 2020-10-08

### Changed
- Renamed `hasVersion` method to `has`
- Better error handling in `patchChangelog` task

## [0.6.0] - 2020-09-29

### Added
- `headerParserRegex` extension property for setting custom regex used to extract version from the header string

### Changed
- Project dependencies upgrade
- Apply ktlint and detekt rules to the code

## [0.5.0] - 2020-09-07

### Added
- `header` extension property for setting new header text
- `date` helper method
- `Helper Methods` section in README

### Removed
- `headerFormat` and `headerArguments` in favor of `header` property

## [0.4.0] - 2020-07-08

### Added
- `initializeChangelog` task for creating new changelog file
- `getAll` extension method for fetching all changelog items
- `groups` extension property for defining the groups created with the Unreleased section
- `ktlint` integration

### Changed
- Move tasks to the `changelog` Gradle group

## [0.3.3] - 2020-07-06

### Added
- `patchEmpty` extension property
- Better error handling for the header parser
- GitHub Actions integration with itself

### Fixed
- Possibility to write date besides versions #5

### Changed
- `unreleasedTerm` default value set from `Unreleased` to `[Unreleased]`

## [0.3.2] - 2020-06-17

### Added
- `markdownToHTML` method in `extensions.kt` file
- `markdownToPlainText` method in `extensions.kt` file

## [0.3.1] - 2020-06-16

### Added
- `--unreleased` flag for the `getChangelog` task

## [0.3.0] - 2020-06-16

### Added
- Allow maintaining changelog without change note types (Added, Fixed)
- Customising the header by the `patchChangelog` task with `headerArguments` extension's property
- Customising the change notes splitting with the `itemPrefix` extension's property
- More tests

### Changed
- `format` extension property renamed to `headerFormat`

### Fixed
- Avoid parsing the unreleased header
- Invalid change notes splitting

## [0.2.0] - 2020-06-09

### Added
- Tests for Plugin, Extension and Tasks
- `getHeader() : String` in `Changelog.Item`
- `withFilter(filter: ((String) -> Boolean)?)` in `Changelog.Item`
- `getLatest()` helper method
- `hasVersion(version: String)` helper method

### Changed
- Extract `closure` to `extensions.kt` separated file
- Code enhancements

## [0.1.5] - 2020-06-04

### Changed
- `changelog.get` and `changelog.getLatest` return `Changelog.Item`
- `noHeader` flag in `Changelog.Item` methods changed to builder pattern
- `Changelog.Item#asHTML` renamed to `Changelog.Item#toHTML`
- `Changelog.Item#asText` renamed to `Changelog.Item#toText`

## [0.1.4] - 2020-06-03

### Fixed
- Remove `org.jetbrains:markdown` dependency from the POM file

## [0.1.3] - 2020-06-01

### Fixed
- Bundle `org.jetbrains:markdown` dependency with `shadowJar`

## [0.1.0] - 2020-05-29

### Added
- Initial release
- `get`/`getUnreleased` helper methods
- `changelog` extension configuration
- `getChangelog`/`patchChangelog` tasks

[Unreleased]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.3.1...v2.0.0
[1.3.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.1.2...v1.2.0
[1.1.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.1...v1.1.1
[1.0.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.6.2...v1.0.0
[0.6.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.6.1...v0.6.2
[0.6.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.6.0...v0.6.1
[0.6.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.3.3...v0.4.0
[0.3.3]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.5...v0.2.0
[0.1.5]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/JetBrains/gradle-changelog-plugin/compare/v0.1.0...v0.1.3
[0.1.0]: https://github.com/JetBrains/gradle-changelog-plugin/commits/v0.1.0
