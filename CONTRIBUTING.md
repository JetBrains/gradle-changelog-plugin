# Contributing to the Gradle Changelog Plugin

There are many ways to contribute to the Gradle Changelog Plugin project, and each of them is valuable to us.
Every submitted feedback, issue, or pull request is highly appreciated.

## Issue Tracker
Before reporting an issue, please update your configuration to use always the [latest release](https://github.com/JetBrains/gradle-changelog-plugin/releases).

If you find your problem unique, and it wasn't yet reported to us, [file an issue](https://github.com/JetBrains/gradle-changelog-plugin/issues/new) using the provided issue template.

## Link With Your Project
It is possible to link your plugin project with the Gradle Changelog Plugin project, so it'll be loaded and built as a module.
To integrate it with another consumer-like project, add the following line in the Gradle settings file and refresh your Gradle configuration:

```kotlin
includeBuild("/path/to/gradle-changelog-plugin")
```

## Pull Requests
To correctly prepare the pull requests, make sure to provide the following information:
- proper title and description of the GitHub Pull Request – describe what your change introduces, what issue it fixes, etc.
- relevant entry in the [`CHANGELOG.md`](https://github.com/JetBrains/gradle-changelog-plugin/blob/master/CHANGELOG.md) file
- unit tests (if necessary)
