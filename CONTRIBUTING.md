# Contributing to Apollo Android

The Apollo team welcomes contributions of all kinds, including bug reports, documentation, test cases, bug fixes, and
features.

If you want to discuss the project or just say hi, stop
by [the kotlinlang slack channel](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)

## Project Setup

This project is developed using IntelliJ IDEA. Android Studio might work too, but we find out the experience for
multiplatform code to be better with IntelliJ IDEA. To build multiplatform projects, you need MacOS and the Xcode
developer tools.

To build the integration tests, use the `tests` build. It's a composite build that includes the main build so that it's
possible to use `apollo-gradle-plugin` with dependency substitution.

## DOs and DON'Ts

### DO:

* Follow our [coding style](#coding-style)
* Add labels to your issues and pull requests (at least one label for each of Status/Type/Priority).
* Give priority to the current style of the project or file you're changing, even if it diverges from the general
  guidelines.
* Include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current
  behavior is broken.
* Keep the discussions focused. When a new or related topic comes up, it's often better to create a new issue than to
  side track the discussion.

### DON'T:

* Send PRs for style changes.
* Surprise us with big pull requests. Instead, file an issue and start a discussion so we can agree on a direction
  before you invest a large amount of time.
* Commit code that you didn't write. If you find code that you think is a good fit, file an issue and start a discussion
  before proceeding.
* Submit PRs that alter licensing related files or headers. If you believe there's a problem with them, file an issue
  and we'll be happy to discuss it.

## Coding Style

The coding style employed here is fairly conventional Kotlin - indentations are 2 spaces, class names are PascalCased,
identifiers and methods are camelCased.

* Use primary constructors when there is at most one optional parameter.
* Use the `Builder` pattern in other places because it's a well recognized pattern that interops well with Java and doesn't copy too much (see https://github.com/apollographql/apollo-android/issues/3301).
* Functions with optional parameters are nice. Use `@JvmOverloads` for better Java interop.
* Interface default function don't support `@JvmOverloads`. (See https://youtrack.jetbrains.com/issue/KT-36102) Try to limit the number of optional parameters when possible.
* Avoid extension functions when possible because they are awkward to use in Java.
* The exception to the above rule is when adding function in other modules. `ApolloClient.Builder` extensions are a good example of that.
* If some extensions do not make sense in Java, mark them with `@JvmName("-$methodName")` to hide them from Java
* Parameters using milliseconds should have the "Millis" suffix.
* Else use [kotlin.time.Duration]
* `ExperimentalContracts` is ok to use. Since kotlin-stdlib does it, we can too. See https://github.com/Kotlin/KEEP/blob/master/proposals/kotlin-contracts.md#compatibility-notice
* `Prefer` top level `val` to top level singleton `objects`. For an example, `Adapters.StringAdapter` reads better in java than `StringAdapter.INSTANCE`
 
## Workflow

We love Github issues!  Before working on any new features, please open an issue so that we can agree on the direction,
and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bugfixes, etc are always welcome.

Please note that we will not accept pull requests for style changes.

## API compatibility

Apollo Android observes [semantic versioning](https://semver.org/). Between major releases, breaking changes are not
allowed and any public API change will fail the build.

If that happens, you will need to run `./gradlew apiDump` and check for any incompatible changes before commiting these
files.

## Experimental / internal APIs

Using Kotlin's (or other dependencies') experimental or internal APIs, such as the ones marked
with `@ExperimentalCoroutinesApi` should be avoided as much as possible (exceptions can be made for native/JS targets only when no other option is
available). Indeed, applications using a certain version of Apollo Android could use a more up-to-date version of these
APIs than the one used when building the library, causing crashes or other issues.

We also have the `@ApolloExperimental` annotation which can be used to mark APIs as experimental, for instance when
feedback is wanted from the community on new APIs. This can also be used as a warning that APIs are using experimental
features of Kotlin/Coroutines/etc. and therefore may break in certain situations.

## Releasing

Releasing is done using Github Actions. The CI contains credentials to upload artifacts to Sonatype and the Gradle
Plugin Portal.

Snapshots are published automatically.  
Releases are published when a tag is pushed.

To create a new tag, use the script:

```bash
scripts/tag.main.kts <version-name>
```

and then push the tag.

This will publish to the Gradle Portal and upload to OSSRH. After a successful CI build, you need to login
to https://oss.sonatype.org/ and release the artifacts manually. This step is called "close, release and drop" in the
Sonatype ecosystem.

After a successful release, do not forget to:
* add the changelog to the [releases page](https://github.com/apollographql/apollo-android/releases).
* merge pending documentation/tutorial updates
* if it's a significant release, tweet about it 🐦
 

## Overview of the CI

The project uses [GitHub Actions](https://docs.github.com/en/actions) to automate the build process.

We have [3 workflows](https://github.com/apollographql/apollo-android/tree/dev-3.x/.github/workflows), triggered by the
following events:

### On PRs

**Workflow:** [`pr.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/pr.yml)

**Jobs (run in parallel):**

- `tests-gradle`
    - Slow gradle tests
- `tests-no-gradle`
    - All root tests but not the Gradle ones and not the "exotic" apple ones (tvos, watchos)
    - All apiCheck
- `tests-integration`
    - All integration tests (except Java 9+ ones)
- `tests-java9`
    - Java 9+ specific tests

### On pushes to `dev-3.x` branch

_(Will be replaced to "on pushes to `main` branch" after v3 is released.)_

**Workflow:** [`push.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/push.yml)

**Job:**

- `push`
    - Runs on macOS
    - Slow
    - Run all tests
    - Publish Snapshot to Sonatype
    - Publish KDoc

### On new tags

**Workflow:** [`tag.yml`](https://github.com/apollographql/apollo-android/blob/dev-3.x/.github/workflows/tag.yml)

**Job:**

- `publish`
    - Publish to Maven Central
