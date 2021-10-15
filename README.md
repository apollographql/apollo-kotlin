
# Apollo Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/main/LICENSE)
[![Join Spectrum](https://img.shields.io/badge/spectrum-join-orange?logo=spectrum)](https://spectrum.chat/apollo/apollo-android)
[![Slack](https://img.shields.io/static/v1?label=kotlinlang&message=apollo-android&color=15a2f5&logo=slack)](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)
[![CI](https://github.com/apollographql/apollo-android/workflows/CI/badge.svg)](https://github.com/apollographql/apollo-android/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)
[![OSS Snapshots](https://img.shields.io/nexus/s/com.apollographql.apollo3/apollo-api?server=https%3A%2F%2Foss.sonatype.org&label=oss-snapshots)](https://oss.sonatype.org/content/repositories/snapshots/com/apollographql/apollo3/)

Apollo Android is a GraphQL client that generates Java and Kotlin models from GraphQL queries. These models give you a type-safe API to work with GraphQL servers.  Apollo helps you keep your GraphQL query statements together, organized, and easy to access.

This library is designed primarily with Android in mind, but you can use it in any Java/Kotlin app.

## Features

* Java and Kotlin code generation
* Queries, Mutations and Subscriptions
* Reflection-free parsing of responses
* HTTP cache
* Normalized cache
* File uploads
* Custom scalar types
* Support for RxJava2, RxJava3, and Coroutines

## Getting started

If you are new to GraphQL, check out [the tutorial](https://www.apollographql.com/docs/android/tutorial/00-introduction/) that will guide you through building an Android app using Apollo, Kotlin and coroutines.

If you'd like to add Apollo Android to an existing project:

* [Get started with Kotlin](https://www.apollographql.com/docs/android/essentials/get-started-kotlin) shows how to add Apollo Android to a Kotlin project.
* [Get started with Java](https://www.apollographql.com/docs/android/essentials/get-started-java) shows how to add Apollo Android to a Java project.
* [Get started with Multiplatform (Experimental)](https://www.apollographql.com/docs/android/essentials/get-started-multiplatform) shows how to add Apollo Android to a Multiplatform project. This is still under heavy development and APIs may change without warning.


## Advanced topics

Check [the project website](https://www.apollographql.com/docs/android/) for in depth documentation about [caching](https://www.apollographql.com/docs/android/essentials/caching/), [plugin configuration](https://www.apollographql.com/docs/android/essentials/plugin-configuration/), [android](https://www.apollographql.com/docs/android/advanced/android/), [file upload](https://www.apollographql.com/docs/android/advanced/file-upload/), [coroutines](https://www.apollographql.com/docs/android/advanced/coroutines/), [rxjava2](https://www.apollographql.com/docs/android/advanced/rxjava2/), [rxjava3](https://www.apollographql.com/docs/android/advanced/rxjava3/), [persisted queries](https://www.apollographql.com/docs/android/advanced/persisted-queries/), [no runtime](https://www.apollographql.com/docs/android/advanced/no-runtime/), [migrations](https://www.apollographql.com/docs/android/essentials/migration/) and much more...

## IntelliJ Plugin

The [JS Graphql IntelliJ Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) provides auto-completion, error highlighting, and go-to-definition functionality for your `.graphql` files. You can create a [`.graphqlconfig`](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files) file to use GraphQL scratch files to work with your schema outside product code (such as to write temporary queries to test resolvers).

## Releases

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)

Check the [changelog](https://github.com/apollographql/apollo-android/releases) for the release history.

Releases are hosted on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo3/). The plugin is additionally hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.apollographql.apollo3)


```groovy:title=build.gradle.kts
plugins {
  id("com.apollographql.apollo3").version("x.y.z")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:x.y.z")

  // optional: if you want to use the normalized cache
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:x.y.z")
  // optional: for coroutines support
  implementation("com.apollographql.apollo3:deprecated-apollo-coroutines-support:x.y.z")
  // optional: for RxJava3 support  
  implementation("com.apollographql.apollo3:deprecated-apollo-rx3-support:x.y.z")
  // optional: if you just want the generated models and parsers and write your own HTTP code/cache code, you can remove apollo-runtime
  // and use apollo-api instead  
  implementation("com.apollographql.apollo3:apollo-api:x.y.z")
}
```

## Snapshots

Latest development changes are available in Sonatype's snapshots repository:

```kotlin:title=build.gradle.kts
repositories {
  maven { 
    url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
  }
}
```

## Multiplatform

Apollo Android is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) project. 

Here's the current matrix of supported features per platform:

|  | `jvm` | `iosX64`, `iosArm64` | `macosX64` | `js` |
| --- | :---: | :---: | :---: | :---: |
| `apollo-api` (models)|✅|✅|✅|✅|
| `apollo-runtime` (network, query batching, apq, ...) |✅|✅|✅|✅¹|
| `apollo-normalized-cache` |✅|✅|✅|✅|
| `apollo-normalized-cache-sqlite` |✅|✅|✅|🚫|
| `apollo-adapters` |✅|✅|✅|✅|
| `apollo-http-cache` |✅|🚫|🚫|🚫|

¹: WebSockets are currently not supported on `js`

## Requirements

Apollo Android runs on the following platforms:

* Android API level 15+
* JDK 8+
* iOS 13+

For building, it requires:

* Gradle 5.6
* Kotlin 1.4+

## Contributing

If you'd like to contribute, please see [Contributing.md](https://github.com/apollographql/apollo-android/blob/main/Contributing.md).

## Additional resources

- [A journey to Kotlin multiplatform](https://www.youtube.com/watch?v=GN6LHrqyimI): how the project was moved to Kotlin multiplatform, talk given at Kotliners in June 2020.
- [#125, Fragmented Podcast](http://fragmentedpodcast.com/episodes/125/): Why's and How's about Apollo Android and the entire journey.
- [GraphQL.org](http://graphql.org) for an introduction and reference to GraphQL itself.
- [apollographql.com](http://www.apollographql.com/) to learn about Apollo open-source and commercial tools.
- [The Apollo blog](https://www.apollographql.com/blog/) for long-form articles about GraphQL, feature announcements for Apollo, and guest articles from the community.
- [The Apollo Twitter account](https://twitter.com/apollographql) for in-the-moment news.