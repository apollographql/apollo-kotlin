---
title: Introduction to Apollo Kotlin
description: A strongly-typed, caching GraphQL client for Java and Kotlin multiplatform
---

> 📣 **Apollo Kotlin 3 is generally available.** If you're using Apollo Android 2.x, see the [migration guide](./migration/3.0/). You can also [view the 2.x docs](https://www.apollographql.com/docs/kotlin/v2).

[Apollo Kotlin](https://github.com/apollographql/apollo-kotlin) (formerly Apollo Android) is a GraphQL client that generates Kotlin and Java models from GraphQL queries.

Apollo Kotlin executes queries and mutations against a GraphQL server and returns results as query-specific Kotlin types. This means you don't have to deal with parsing JSON, or passing around `Map`s and making clients cast values to the right type manually. You also don't have to write model types yourself, because these are generated from the GraphQL definitions your UI uses.

Because generated types are query-specific, you can only access data that you actually specify as part of a query. If you don't ask for a particular field in a query, you can't access the corresponding property on the returned data structure.

This library is designed primarily with Android in mind, but you can use it in any Java/Kotlin app, including multiplatform.

## Features

* Java and Kotlin Multiplatform code generation
* Queries, Mutations and Subscriptions
* Reflection-free parsing
* Normalized cache
* Custom scalar types
* HTTP cache
* Auto Persisted Queries
* Query batching
* File uploads
* Fake models for tests
* AppSync and graphql-ws websockets
* GraphQL AST parser

## Multiplatform

Apollo Kotlin is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) project.

Here's the current matrix of supported features per platform:

|  | `jvm` | `Apple¹` | `js` | `linuxX64`
| --- | :---: | :---: |:----:| :---: |
| `apollo-api` (models)|✅|✅|  ✅   |✅|
| `apollo-runtime` (network, query batching, apq, ...) |✅|✅|  ✅   |🚫|
| `apollo-normalized-cache` |✅|✅|  ✅   |🚫|
| `apollo-adapters` |✅|✅|  ✅   |🚫|
| `apollo-normalized-cache-sqlite` |✅|✅|  🚫  |🚫|
| `apollo-http-cache` |✅|🚫|  🚫  |🚫|

¹: Apple currently includes:

- `macosX64`
- `macosArm64`
- `iosArm64`
- `iosX64`
- `iosSimulatorArm64`
- `watchosArm32`
- `watchosArm64`
- `watchosSimulatorArm64`
- `tvosArm64`
- `tvosX64`
- `tvosSimulatorArm64`

## Getting started

If you are new to GraphQL, check out [the tutorial](./tutorial/00-introduction/) that will guide you through building an Android app using Apollo, Kotlin and coroutines.

If you'd like to add Apollo Kotlin to an existing project, follow these steps:

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("com.apollographql.apollo3") version "4.0.0-beta.7"
}
```

Add the runtime dependency:

```kotlin
dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:4.0.0-beta.7")
}
```

Set the package name to use for the generated models:

```kotlin
apollo {
  service("service") {
    packageName.set("com.example")
  }
}
```

Apollo Kotlin supports three types of files:
- `.graphqls` schema files: describes the types in your backend using the GraphQL syntax.
- `.json` schema files: describes the types in your backend using the Json syntax.
- `.graphql` executable files: describes your queries and operations in the GraphQL syntax.

By default, Apollo Kotlin requires a schema in your module's `src/main/graphql` directory. You can download a schema using introspection with the `./gradlew downloadApolloSchema` task. Sometimes introspection is disabled and you will have to ask your backend team to provide a schema. Copy this schema to your module:

```
cp ${schema} ${module}/src/main/graphql/
```

Write a query in a `${module}/src/main/graphql/GetRepository.graphql` file:

```graphql
query HeroQuery($id: String!) {
  hero(id: $id) {
    id
    name
    appearsIn
  }
}
```

Build your project. This will generate a `HeroQuery` class that you can use with an instance of `ApolloClient`:

```kotlin
  // Create a client
  val apolloClient = ApolloClient.Builder()
      .serverUrl("https://example.com/graphql")
      .build()

  // Execute your query. This will suspend until the response is received.
  val response = apolloClient.query(HeroQuery(id = "1")).execute()

  println("Hero.name=${response.data?.hero?.name}")
```

**To learn more about other Apollo Kotlin APIs:**

* Execute your first [mutation](./essentials/mutations/)
* Handle [custom scalar types](./essentials/custom-scalars/)
* Factor common patterns using [fragments](./essentials/fragments/)

## Requirements

Some platforms have specific runtime requirements:

* JVM 8+
* Android API level 21+ (`apollo-http-cache` and `apollo-adapters` require enabling [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) on Android API levels < 26)
* iOS 13+

At build time, it requires:

* Gradle 8.0+
* Kotlin 1.9+ for JVM projects
* Kotlin 2.0+ for native projects
* Kotlin 2.0+ for JS projects

## Proguard / R8 configuration

As the code generated by Apollo Kotlin doesn't use any reflection, it can safely be optimized / obfuscated by Proguard or R8, so no particular exclusions need to be configured.

## Android Studio / IntelliJ plugin

An experimental [plugin for Android Studio and IntelliJ](https://plugins.jetbrains.com/plugin/20645-apollo-graphql) is available to help you work with Apollo Kotlin, providing automatic code generation, integration with the [GraphQL IntelliJ Plugin](https://plugins.jetbrains.com/plugin/8097-js-graphql), navigation to GraphQL definitions, migration helpers, and more.

Installation instructions and more information can be found [here](testing/android-studio-plugin).

## Releases

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)

Check the [changelog](https://github.com/apollographql/apollo-kotlin/releases) for the release history.

Releases are hosted on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo3/). The plugin is additionally hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.apollographql.apollo3)


```kotlin
plugins {
  id("com.apollographql.apollo3") version "4.0.0-beta.7"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:4.0.0-beta.7")

  // optional: if you want to use the normalized cache
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:4.0.0-beta.7")
  // optional: if you just want the generated models and parsers and write your own HTTP code/cache code, you can remove apollo-runtime
  // and use apollo-api instead
  implementation("com.apollographql.apollo3:apollo-api:4.0.0-beta.7")
}
```

## Snapshots

Latest development changes are available in Sonatype's snapshots repository:

```kotlin
// build.gradle.kts
repositories {
  maven {
    url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
  mavenCentral()
  // other repositories...
}

// settings.gradle.kts
pluginManagement {
  repositories {
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    gradlePluginPortal()
    mavenCentral()
    // other repositories...
  }
}
```
And then use the `4.0.0-beta.8-SNAPSHOT` version for the plugin and libraries.

These snapshots are updated on each push to `main`.

Weekly snapshots for the Android Studio / IntelliJ plugin [are also available](testing/android-studio-plugin#weekly-snapshots). 

## Stability of different artifacts

Apollo Kotlin is very modular and publishes several artifacts. 

* Artifacts ending with `-incubating` are not finalized yet and subject to change any time.
* Other artifacts observe [Semantic Versioning](https://semver.org/). 
  * No breaking change should be introduced in minor or patch releases except for symbols annotated with `@ApolloExperimental` that are subject to change at any time.
  * Deprecated symbols may be removed in the next major release. We strongly recommend removing deprecated usages before migrating to the next major version.

## Contributing

If you'd like to contribute, please see [Contributing.md](https://github.com/apollographql/apollo-kotlin/blob/main/CONTRIBUTING.md).

## Community integrations

* If you're using the [Maven](https://maven.apache.org/) build tool, [apollo-client-maven-plugin](https://github.com/aoudiamoncef/apollo-client-maven-plugin) is a Maven plugin that calls the Apollo Kotlin compiler to generate your Java/Kotlin sources.
* If you're using [Absinthe Phoenix subscriptions](https://hexdocs.pm/absinthe_phoenix/readme.html), [kotlin-phoenix](https://github.com/ajacquierbret/kotlin-phoenix) has a [PhoenixNetworkTransport](https://github.com/ajacquierbret/kotlin-phoenix/blob/main/kotlinphoenix-adapters/src/commonMain/kotlin/io/github/ajacquierbret/kotlinphoenix/adapters/apollo/PhoenixNetworkTransport.kt) that you can use together with `ApolloClient` ([doc](https://github.com/ajacquierbret/kotlin-phoenix/tree/main/kotlinphoenix-adapters))

## Additional resources

- [Confetti](https://github.com/joreilly/Confetti): A Kotlin Multiplatform conference app using Apollo Kotlin, SwiftUI and Jetpack Compose.
- [MortyComposeKMM](https://github.com/joreilly/MortyComposeKMM): A Kotlin Multiplatform Github template using Apollo Kotlin, SwiftUI and Jetpack Compose.
- [A journey to Kotlin multiplatform](https://www.youtube.com/watch?v=GN6LHrqyimI): how the project was moved to Kotlin multiplatform, talk given at Kotliners in June 2020.
- [#125, Fragmented Podcast](http://fragmentedpodcast.com/episodes/125/): Why's and How's about Apollo Kotlin and the entire journey.
- [GraphQL.org](http://graphql.org) for an introduction and reference to GraphQL itself.
- [apollographql.com](http://www.apollographql.com/) to learn about Apollo open-source and commercial tools.
- [The Apollo blog](https://www.apollographql.com/blog/) for long-form articles about GraphQL, feature announcements for Apollo, and guest articles from the community.
- [The Apollo Twitter account](https://twitter.com/apollographql) for in-the-moment news.
