# Contributing to Apollo Kotlin

The Apollo team welcomes contributions of all kinds, including bug reports, documentation, test cases, bug fixes, and
features.

If you want to discuss the project or just say hi, stop by [the kotlinlang slack channel](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your invite [here](https://slack.kotl.in/))

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

Builders/Constructors
* Use primary constructors with `@JvmOverloads` when there is at most one optional parameter.
* For classes, nest the builder directly under the class
* For interfaces that are meant to be extended by the user but that also have a builtin implementation, you can use the `Default${Interface}` naming pattern (see DefaultUpload)
* If there are several builtin implementations, use a descriptive name (like AppSyncWsProtocol, ...)
* Avoid top level constructor functions like `fun CoroutineScope(){}` because they are awkward to use in Java
* For expect/actual, it's sometime convenient to expose an interface even if it's not intended to be used by callers like `MockerServerInterface`. In that case, it's ok to use `FooInterface` for the interface and `Foo()` for the implementation to avoid having "DefaultFoo" everywhere when there's only one "Foo". 

Java interop
* In general, it's best to avoid extension functions when possible because they are awkward to use in Java.
* The exception to the above rule is when adding function in other modules. `ApolloClient.Builder` extensions are a good example of that.
* If you have to use extension functions, tweak the `@file:JvmName()` annotation to make the Java callsite nicer
* Avoid Interface default functions as they generate `DefaultImpl` bytecode (and `-Xjvm-default=enable` is not ready for showtime yet).
  * Use abstract classes when possible.
  * Else use extension functions.
* Functions with optional parameters are nice. Use `@JvmOverloads` for better Java interop.
* `Prefer` top level `val` to top level singleton `objects`. For an example, `Adapters.StringAdapter` reads better in java than `StringAdapter.INSTANCE`
* If some extensions do not make sense in Java, mark them with `@JvmName("-$methodName")` to hide them from Java

Logging & Error messages
* Apollo Kotlin must not log anything to System.out or System.err
* Error messages are passed to the user through `Exception.message`
* For debugging logs, APIs are provided to get diagnostics (like CacheMissException, HttpInfo, ...). APIs are better defined and allow more fine-grained diagnostics. 
* There is one exception for the Gradle plugin. It is allowed to log information though the lifecycle() methods. 
* Messages should contain "Apollo: " when it's not immediately clear that the message comes from Apollo.

Misc
* Parameters using milliseconds should have the "Millis" suffix.
* Else use [kotlin.time.Duration]
* `ExperimentalContracts` is ok to use. Since kotlin-stdlib does it, we can too. See https://github.com/Kotlin/KEEP/blob/master/proposals/kotlin-contracts.md#compatibility-notice
 
## Workflow

We love Github issues!  Before working on any new features, please open an issue so that we can agree on the direction,
and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bugfixes, etc are always welcome.

Please note that we will not accept pull requests for style changes.

## API compatibility

Apollo Kotlin observes [semantic versioning](https://semver.org/). Between major releases, breaking changes are not
allowed and any public API change will fail the build.

If that happens, you will need to run `./gradlew apiDump` and check for any incompatible changes before commiting these
files.

## Experimental / internal APIs

Using Kotlin's (or other dependencies') experimental or internal APIs, such as the ones marked
with `@ExperimentalCoroutinesApi` should be avoided as much as possible (exceptions can be made for native/JS targets only when no other option is
available). Indeed, applications using a certain version of Apollo Kotlin could use a more up-to-date version of these
APIs than the one used when building the library, causing crashes or other issues.

We also have the `@ApolloExperimental` annotation which can be used to mark APIs as experimental, for instance when
feedback is wanted from the community on new APIs. This can also be used as a warning that APIs are using experimental
features of Kotlin/Coroutines/etc. and therefore may break in certain situations.

## Releasing

Releasing is done using Github Actions. The CI contains credentials to upload artifacts to Sonatype and the Gradle
Plugin Portal.

Snapshots are published automatically.  
Releases are published when a tag is pushed.

Here are the steps to do a new release:

* `scripts/tag.main.kts <version-name>`
* push the tag and branch
* while it compiles, prepare the changelog, open a PR to `CHANGELOG.md`
* wait for the CI to finish compiling
* go to https://oss.sonatype.org/, and release the artifacts manually. This step is called "close, release and drop" in the Sonatype ecosystem.
* wait for it to be visible on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo3/) (this usually takes a few minutes). If you're on MacOS, you can use [dependency-watch](https://github.com/JakeWharton/dependency-watch): `dependency-watch await 'com.apollographql.apollo3:apollo-runtime:$version' && osascript -e 'display notification "Release is ready 🚀"'`
* paste the changelog in a new release on [GitHub](https://github.com/apollographql/apollo-android/releases)
* merge pending documentation/tutorial updates
* if it's a significant release, tweet about it 🐦
* relax 🍹

## Overview of the CI

The project uses [GitHub Actions](https://docs.github.com/en/actions) to automate the build process.

We have [3 workflows](https://github.com/apollographql/apollo-android/tree/main/.github/workflows), triggered by the
following events:

### On PRs

**Workflow:** [`pr.yml`](https://github.com/apollographql/apollo-android/blob/main/.github/workflows/pr.yml)

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

### On pushes to `main` branch

_(Will be replaced to "on pushes to `main` branch" after v3 is released.)_

**Workflow:** [`push.yml`](https://github.com/apollographql/apollo-android/blob/main/.github/workflows/push.yml)

**Job:**

- `push`
    - Runs on macOS
    - Slow
    - Run all tests
    - Publish Snapshot to Sonatype
    - Publish KDoc

### On new tags

**Workflow:** [`tag.yml`](https://github.com/apollographql/apollo-android/blob/main/.github/workflows/tag.yml)

**Job:**

- `publish`
    - Publish to Maven Central
