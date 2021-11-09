# Contributing to Apollo Android GraphQL

The Apollo team welcomes contributions of all kinds, including bug reports, documentation, test cases, bug fixes, and features.

If you have a usage question, please ask on [Stack Overflow](https://stackoverflow.com/) using the tag `apollo-android`.

## Project Setup

This project is developed using either IntelliJ IDEA or Android Studio. To build multiplatform projects, you need MacOS and the Xcode developer tools.
 
To build the integration tests, use the `tests` build. It's a composite build that includes the main build so that it's possible to use `apollo-gradle-plugin` with dependency substitution.

## DOs and DON'Ts

### DO:

* Follow our [coding style](#coding-style)
* Add labels to your issues and pull requests (at least one label for each of Status/Type/Priority).
* Give priority to the current style of the project or file you're changing, even if it diverges from the general guidelines.
* Include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current behavior is broken.
* Keep the discussions focused. When a new or related topic comes up, it's often better to create a new issue than to side track the discussion.
* Run all Gradle verification tasks (`./gradlew check`) before submitting a pull request.
* Run `./gradlew apiDump` when changing the public API so that API compatibility can be enforced.

### DON'T:

* Send PRs for style changes.
* Surprise us with big pull requests. Instead, file an issue and start a discussion so we can agree on a direction before you invest a large amount of time.
* Commit code that you didn't write. If you find code that you think is a good fit, file an issue and start a discussion before proceeding.
* Submit PRs that alter licensing related files or headers. If you believe there's a problem with them, file an issue and we'll be happy to discuss it.


## Coding Style

The coding style employed here is fairly conventional Kotlin - indentations are 2 spaces, class
names are PascalCased, identifiers and methods are camelCased.    

## Workflow

We love Github issues!  Before working on any new features, please open an issue so that we can agree on the
direction, and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bugfixes, etc are always welcome.

Please note that we will not accept pull requests for style changes.

## Releasing

Prior to releasing a new version, run `./gradlew japicmp` and check what API may have changed. We ideally want 100% API and ABI compatibility but the project is still moving fast and it's not always possible.

To create a new tag, use the script:
```bash
scripts/tag.main.kts <version-name>
```
and then push it.

The CI contains credentials and will push artifacts to sonatype/gradlePortal when a tag is pushed. Every tag will trigger a new release.

After a successful release, do not forget to add a changelog to the [releases page](https://github.com/apollographql/apollo-android/releases).


## Overview of the CI

The project uses [GitHub Actions](https://docs.github.com/en/actions) to automate the build process.

We have [3 workflows](https://github.com/apollographql/apollo-android/tree/dev-3.x/.github/workflows), triggered by the following events:

### On PRs

**Workflow:** [`pr.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/pr.yml)

**Jobs:**
- `tests-gradle`
    - Slow gradle tests
- `tests-no-gradle`
    - All root tests but not the Gradle ones and not the "exotic" apple ones (tvos, watchos)
    - All apiCheck
- `tests-integration`
    - All integration tests

### On pushes to `dev-3.x` branch

_(Will be replaced to "on pushes to `main` branch" after v3 is released.)_

**Workflow:** [`push.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/push.yml)

**Jobs:**
- `tests-all`
    - Runs on macOS
    - Slow
    - Run all tests
- `gh-pages`
    - Publish KDoc
- `snapshot`
    - Publish Snapshot to Sonatype

### On new tags

**Workflow:** [`tag.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/tag.yml)

**Jobs:**
- `publish`
  - Publish to Maven Central
