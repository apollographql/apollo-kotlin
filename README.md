
# Apollo Kotlin

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-kotlin/main/LICENSE)
[![Join the community](https://img.shields.io/discourse/status?label=Join%20the%20community&server=https%3A%2F%2Fcommunity.apollographql.com)](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
[![Slack](https://img.shields.io/static/v1?label=kotlinlang&message=apollo-kotlin&color=15a2f5&logo=slack)](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)
[![Discord](https://img.shields.io/discord/1022972389463687228.svg?color=7389D8&labelColor=6A7EC2&logo=discord&logoColor=ffffff&style=flat-square)](https://discord.com/invite/graphos)
[![CI](https://img.shields.io/github/actions/workflow/status/apollographql/apollo-kotlin/push.yml?branch=main)](https://github.com/apollographql/apollo-kotlin/actions/workflows/push.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)
[![Gradle Plugin](https://img.shields.io/gradle-plugin-portal/v/com.apollographql.apollo3)](https://plugins.gradle.org/plugin/com.apollographql.apollo3)
[![OSS Snapshots](https://img.shields.io/nexus/s/com.apollographql.apollo3/apollo-api?server=https%3A%2F%2Fs01.oss.sonatype.org&label=oss-snapshots)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/apollographql/apollo3/)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.apollographql.com/scans)

| ☑️  Apollo Clients User Survey |
| :----- |
| What do you like best about Apollo Kotlin? What needs to be improved? Please tell us by taking a [one-minute survey](https://docs.google.com/forms/d/e/1FAIpQLSczNDXfJne3ZUOXjk9Ursm9JYvhTh1_nFTDfdq3XBAFWCzplQ/viewform?usp=pp_url&entry.1170701325=Apollo+Kotlin&entry.204965213=Readme). Your responses will help us understand Apollo Kotlin usage and allow us to serve you better. |

Apollo Kotlin (formerly known as Apollo Android) is a GraphQL client that generates Kotlin and Java models from GraphQL queries.

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

## Maintainers

* [martinbonnin](https://github.com/martinbonnin)
* [BoD](https://github.com/BoD)
* [@bignimbus](https://github.com/bignimbus)

## Documentation

Check [the project website](https://www.apollographql.com/docs/android/) for in depth documentation.

## Getting started

If you are new to GraphQL, check out [the tutorial](https://www.apollographql.com/docs/android/tutorial/00-introduction/) that will guide you through building an Android app using Apollo, Kotlin and coroutines.

If you'd like to add Apollo Kotlin to an existing project, follow these steps:

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("com.apollographql.apollo3") version "4.0.0-beta.6"
}
```

Add the runtime dependency:

```kotlin
dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:4.0.0-beta.6")
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

* Execute your first [mutation](https://www.apollographql.com/docs/kotlin/essentials/mutations/)
* Handle [custom scalar types](https://www.apollographql.com/docs/kotlin/essentials/custom-scalars/)
* Factor common patterns using [fragments](https://www.apollographql.com/docs/kotlin/essentials/fragments/)

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

Installation instructions and more information can be found [here](https://github.com/apollographql/apollo-kotlin/tree/main/intellij-plugin).

## Releases

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)

Check the [changelog](https://github.com/apollographql/apollo-kotlin/releases) for the release history.

Releases are hosted on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo3/). The plugin is additionally hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.apollographql.apollo3)

```kotlin
plugins {
  id("com.apollographql.apollo3") version "4.0.0-beta.6"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:4.0.0-beta.6")

  // optional: if you want to use the normalized cache
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite:4.0.0-beta.6")
  // optional: if you just want the generated models and parsers and write your own HTTP code/cache code, you can remove apollo-runtime
  // and use apollo-api instead
  implementation("com.apollographql.apollo3:apollo-api:4.0.0-beta.6")
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

And then use the `4.0.0-beta.7-SNAPSHOT` version for the plugin and libraries.

These snapshots are updated on each push to `main`.

Weekly snapshots for the Android Studio / IntelliJ plugin [are also available](tree/main/intellij-plugin#weekly-snapshots). 

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

## Who is Apollo?

[Apollo](https://apollographql.com/) builds open-source software and a graph platform to unify GraphQL across your apps and services. We help you ship faster with:

* [Apollo Studio](https://www.apollographql.com/studio/develop/) – A free, end-to-end platform for managing your GraphQL lifecycle. Track your GraphQL schemas in a hosted registry to create a source of truth for everything in your graph. Studio provides an IDE (Apollo Explorer) so you can explore data, collaborate on queries, observe usage, and safely make schema changes.
* [Apollo Federation](https://www.apollographql.com/apollo-federation) – The industry-standard open architecture for building a distributed graph. Use Apollo’s gateway to compose a unified graph from multiple subgraphs, determine a query plan, and route requests across your services.
* [Apollo Client](https://www.apollographql.com/apollo-client/) – The most popular GraphQL client for the web. Apollo also builds and maintains [Apollo iOS](https://github.com/apollographql/apollo-ios) and [Apollo Kotlin](https://github.com/apollographql/apollo-kotlin).
* [Apollo Server](https://www.apollographql.com/docs/apollo-server/) – A production-ready JavaScript GraphQL server that connects to any microservice, API, or database. Compatible with all popular JavaScript frameworks and deployable in serverless environments.

## Learn how to build with Apollo

Check out the [Odyssey](https://odyssey.apollographql.com/) learning platform, the perfect place to start your GraphQL journey with videos and interactive code challenges. Join the [Apollo Community](https://community.apollographql.com/) to interact with and get technical help from the GraphQL community.
