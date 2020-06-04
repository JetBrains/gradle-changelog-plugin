# Gradle Changelog Plugin

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
