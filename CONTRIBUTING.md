# Contributing to Apollo Kotlin

The Apollo team welcomes contributions of all kinds, including bug reports, documentation, test cases, bug fixes, and
features.

If you want to discuss the project or just say hi, stop
by [the kotlinlang Slack channel](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your
invite [here](https://slack.kotl.in/))

## Project Setup

You will need:

* A Java17+ JDK installed locally on your machine. 
* A recent version of IntelliJ IDEA community. Android Studio might work too, but we find out the experience for
  multiplatform code to be better with IntelliJ IDEA.
* MacOS and the Xcode developer tools for iOS/MacOS targets.
* Simulators for iOS/watchOS tests.

## Composite builds

This repository contains several Gradle builds:

* root build: the main libraries
* `build-logic`: the shared Gradle logic
* `tests`: integration tests
* `benchmarks`: Android micro and macro benchmarks

We recommend opening the `tests` folder in IntelliJ. It's a composite build that includes the main build and
integration-tests, so it's easy to add GraphQL and test the codegen end-to-end. If you only want to do small changes,
you can open the root project to save some sync times.

## Using a local version of Apollo Kotlin

To test your changes in a local repo, you can publish a local version of `apollo-gradle-plugin` and other dependencies
with:

```
./gradlew publishToMavenLocal
```

All dependencies will be published to your `~/.m2/repository` folder. You can then use them in other projects by
adding `mavenLocal()`
to your repositories in your build scripts:

```kotlin
// build.gradle.kts
repositories {
  mavenLocal()
  mavenCentral()
  // other repositories...
}

// settings.gradle.kts
pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
    // other repositories...
  }
}
```

This will require that you call `./gradlew publishToMavenLocal` after every change you make in Apollo Kotlin but it's
the
easiest way to try your changes. For tighter integration, you can also use Apollo Kotlin as
an [included build](https://docs.gradle.org/current/userguide/composite_builds.html)
like it is done for the integration-tests.

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

We usually favor Builders for reasons outlined in [this issue](https://github.com/apollographql/apollo-kotlin/issues/3301) unless there is only one optional parameter.

* Use primary constructors with `@JvmOverloads` when there is at most one optional parameter.
* For classes, nest the builder directly under the class
* For interfaces that are meant to be extended by the user but that also have a builtin implementation, you can use
  the `Default${Interface}` naming pattern (see DefaultUpload)
* If there are several builtin implementations, use a descriptive name (like AppSyncWsProtocol, ...)
* Avoid top level constructor functions like `fun CoroutineScope(){}` because they are awkward to use in Java
* For expect/actual, it's sometime convenient to expose an interface even if it's not intended to be subclassed by
  callers like `MockServerInterface`. In that case, it's ok to use `FooInterface` for the interface and `Foo()` for the
  implementation to avoid having "DefaultFoo" everywhere when there's only one "Foo".
* Builders may open the door to bad combination of arguments. This is ok. In that case, they should be detected at
  runtime and fail fast by throwing an exception.

Java interop

* In general, it's best to avoid extension functions when possible because they are awkward to use in Java.
* The exception to the above rule is when adding function in other modules. `ApolloClient.Builder` extensions are a good
  example of that.
* If you have to use extension functions, tweak the `@file:JvmName()` annotation to make the Java callsite nicer
* Avoid Interface default functions as they generate `DefaultImpl` bytecode (and `-Xjvm-default=enable` is not ready for
  showtime yet).
    * Use abstract classes when possible.
    * Else use extension functions.
* Functions with optional parameters are nice. Use `@JvmOverloads` for better Java interop.
* `Prefer` top level `val` to top level singleton `objects`. For an example, `Adapters.StringAdapter` reads better in
  java than `StringAdapter.INSTANCE`
* If some extensions do not make sense in Java, mark them with `@JvmName("-$methodName")` to hide them from Java

Logging & Error messages

* Apollo Kotlin must not log anything to System.out or System.err
* Error messages are passed to the user through `Exception.message`
* For debugging logs, APIs are provided to get diagnostics (like CacheMissException, HttpInfo, ...). APIs are better
  defined and allow more fine-grained diagnostics. See  https://publicobject.com/2022/05/01/eventlisteners-are-good/
* There is one exception for the Gradle plugin. It is allowed to log information though the lifecycle() methods.
* Messages should contain "Apollo: " when it's not immediately clear that the message comes from Apollo.

Gradle APIs

Gradle is a bit peculiar because it can be used from both Kotlin and Groovy, has lazy and eager APIs and can sometimes
be used as a DSL and sometimes imperatively. The rules we landed on are:

* Lazy properties use names. Example: `packageName.set("com.example")`
* Methods with one or more parameters use verbs. Example: `mapScalar("ID", "kotlin.Long")`
* Except when there is only one parameter that is of `Action<T>` type. Example: `introspection {}`

Misc

* Parameters using milliseconds should have the "Millis" suffix.
* Else use [kotlin.time.Duration]
* `ExperimentalContracts` is ok to use. Since kotlin-stdlib does it, we can too.
  See https://github.com/Kotlin/KEEP/blob/master/proposals/kotlin-contracts.md#compatibility-notice

## Workflow

We love GitHub issues!  Before working on any new features, please open an issue so that we can agree on the direction,
and hopefully avoid investing a lot of time on a feature that might need reworking.

Small pull requests for things like typos, bugfixes, etc are always welcome.

Please note that we will not accept pull requests for style changes.

## API evolution

Apollo Kotlin observes [semantic versioning](https://semver.org/). No breaking change should be introduced in minor or patch releases. See our [evolution policy](https://www.apollographql.com/docs/kotlin/v4/essentials/evolution) for more details.

The public API is tracked thanks to the [Binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator) plugin.

Any change to the public API will fail the build. If that happens, you will need to run `./gradlew apiDump` and check for any incompatible changes before committing these
files.

## Deprecation

When deprecating an API, also mark it with `ApolloDeprecatedSince` so we can keep track of when it has been deprecated.

In general, when an existing API must be deprecated, use the `WARNING` level. Use the `replaceWith` parameter to guide the developer to an alternative API to use. This can happen in a minor release (not a breaking change).

The API can then be removed in the next major release (breaking change).

```mermaid
stateDiagram-v2
    direction LR
    NotDeprecated: Not deprecated
    NotDeprecated --> Deprecated(WARNING): Minor release
    Deprecated(WARNING) --> Removed: Major release
```

## Experimental / internal APIs

Using Kotlin's (or other dependencies') experimental or internal APIs, such as the ones marked
with `@ExperimentalCoroutinesApi` should be avoided as much as possible.
We have historically made exceptions to that rule for JS/native (`UnsafeNumber`) but no new opt-in to experimental APIs must be made.

We also have the `@ApolloExperimental` annotation which can be used to mark APIs as experimental, for instance when
feedback is wanted from the community on new APIs. This can also be used as a warning that APIs are using experimental
features of Kotlin/Coroutines/etc. and therefore may break in certain situations.

## Releasing

Releasing is done using Github Actions. The CI contains credentials to upload artifacts to Sonatype and the Gradle
Plugin Portal.

Snapshots are published automatically.  
Releases are published when a tag is pushed.

Here are the steps to do a new release:

* `git checkout main && git pull`
* `scripts/release.main.kts <version-name>`
* while it compiles, prepare the changelog, open a PR to `CHANGELOG.md` (see below)
* wait for the CI to finish compiling
* go to https://s01.oss.sonatype.org/, and release the artifacts manually. This step is called "close, release and drop"
  in the Sonatype ecosystem.
* wait for it to be visible on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo/) (this usually
  takes a few minutes). If you're on MacOS, you can
  use [dependency-watch](https://github.com/JakeWharton/dependency-watch): `dependency-watch await 'com.apollographql.apollo:apollo-runtime:$version' && osascript -e 'display notification "Release is ready 🚀"'`
* merge pending documentation/tutorial updates. Make sure the tutorial compiles and runs well.
* paste the changelog in a new release on [GitHub](https://github.com/apollographql/apollo-kotlin/releases)
* if it's a significant release, tweet about it 🐦
* relax 🍹

### Changelog file
* Add a section with the version, date, and a quick summary of what the release contains.
* Optionally add a few sections to zoom in on changes you want to highlight.
* No need to highlight deprecations, as warnings in the code are enough.
* Mention and thank external contributors if any.
* Add an "All changes" section that should list all commits since last release (can use `git log --pretty=oneline <previous-tag>..main`). Commits on the documentation can be omitted.

## Debugging minimized Gradle Plugin stacktraces

Because the Gradle plugin uses [R8](https://r8.googlesource.com/r8) to relocate dependencies, the stacktraces do not
match the source code by default. It is possible to retrace them using the mapping file and R8.

Indicative steps (replace values accordingly below):

```
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
export PATH=/path/to/depot_tools:$PATH
git clone https://r8.googlesource.com/r8
cd r8 
./tools/gradle.py d8 r8
wget https://repo.maven.apache.org/maven2/com/apollographql/apollo3/apollo-gradle-plugin/3.3.1/apollo-gradle-plugin-3.3.1-mapping.txt
java -cp build/libs/r8_with_deps.jar com.android.tools.r8.retrace.Retrace apollo-gradle-plugin-3.3.1-mapping.txt
[copy paste your stacktrace and press Crtl-D to launch the retracing]
```

## Tests

You can run tests with `./gradlew build`.

Because the Apollo Kotlin compiler runs from a Gradle Plugin, a lot of integration tests are in the `tests` composite build.

You can run integration tests with `./gradlew -p tests build`

Gradle tests are slow and not always easy to debug in the IDE. Most of the times,
an integration test from the `tests` composite build is faster. Keep Gradle tests for the cases
where:

* We need to test a specific version of Gradle and/or KGP, AGP or another buildscript dependency.
* We need to tweak the Gradle environment, for an example, a server needs to run in the background.
* We need to test up-to-date checks, build cache or other things that require instrumenting the build outcome.

## Overview of the CI

The project uses [GitHub Actions](https://docs.github.com/en/actions) to automate the build process.

We have [3 workflows](https://github.com/apollographql/apollo-kotlin/tree/main/.github/workflows), triggered by the
following events:

### On PRs

**Workflow:** [`pr.yml`](https://github.com/apollographql/apollo-kotlin/blob/main/.github/workflows/pr.yml)

**Jobs (run in parallel):**

- `tests-gradle`
    - Slow gradle tests
- `tests-no-gradle`
    - All root tests but not the Gradle ones and not the "exotic" apple ones (tvos, watchos)
    - All apiCheck
- `tests-integration`
    - All integration tests (except Java 9+ ones)
- `intellij-plugin`
    - IntelliJ plugin build and tests

### On pushes to `main` branch

**Workflow:** [`push.yml`](https://github.com/apollographql/apollo-kotlin/blob/main/.github/workflows/push.yml)

**Job:**

- `deploy`
    - Runs on macOS
    - Slow
    - Run all tests
    - Publish Snapshot to Sonatype
    - Publish KDoc

### On new tags

**Workflow:** [`tag.yml`](https://github.com/apollographql/apollo-kotlin/blob/main/.github/workflows/tag.yml)

**Job:**

- `publish-libraries`
    - Publish libraries to Maven Central
- `publish-intellij-plugin`
    - Publish IntelliJ plugin to Jetbrains Marketplace
