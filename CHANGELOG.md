# Gradle Changelog Plugin

## [0.3.1]
### Added
- `--unreleased` flag for the `getChangelog` task

## [0.3.0]
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

## [0.2.0]
### Added
- Tests for Plugin, Extension and Tasks
- `getHeader() : String` in `Changelog.Item`
- `withFilter(filter: ((String) -> Boolean)?)` in `Changelog.Item`
- `getLatest()` helper method
- `hasVersion(version: String)` helper method

### Changed
- Extract `closure` to `extensions.kt` separated file
- Code enhancements

## [0.1.5]
### Changed
- `changelog.get` and `changelog.getLatest` return `Changelog.Item`
- `noHeader` flag in `Changelog.Item` methods changed to builder pattern
- `Changelog.Item#asHTML` renamed to `Changelog.Item#toHTML` 
- `Changelog.Item#asText` renamed to `Changelog.Item#toText` 

## [0.1.4]
### Fixed
- Remove `org.jetbrains:markdown` dependency from the POM file

## [0.1.3]
### Fixed
- Bundle `org.jetbrains:markdown` dependency with `shadowJar`

## [0.1.0]
### Added
- Initial release
- `get`/`getUnreleased` helper methods
- `changelog` extension configuration
- `getChangelog`/`patchChangelog` tasks
