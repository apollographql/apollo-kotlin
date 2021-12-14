---
title: Introduction to Apollo Kotlin
description: A strongly-typed, caching GraphQL client for Java and Kotlin multiplatform
sidebar_title: Introduction
---

[Apollo Kotlin](https://github.com/apollographql/apollo-android) (formerly Apollo Android) is a GraphQL client that generates Kotlin and Java models from GraphQL queries.

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
* Espresso IdlingResource
* Fake models for tests
* AppSync and graphql-ws websockets
* GraphQL AST parser

## Multiplatform

Apollo Kotlin is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) project.

Here's the current matrix of supported features per platform:

|  | `jvm` | Apple¹ | `js` | `linuxX64`
| --- | :---: | :---: | :---: | :---: |
| `apollo-api` (models)|✅|✅|✅|✅|
| `apollo-runtime` (network, query batching, apq, ...) |✅|✅|✅²|🚫|
| `apollo-normalized-cache` |✅|✅|✅|🚫|
| `apollo-adapters` |✅|✅|✅|🚫|
| `apollo-normalized-cache-sqlite` |✅|✅|🚫|🚫|
| `apollo-http-cache` |✅|🚫|🚫|🚫|

¹: Apple currently includes:

- `macosX64`
- `macosArm64`
- `iosArm64`
- `iosX64`
- `iosSimulatorArm64`
- `watchosArm64`
- `watchosSimulatorArm64`
- `tvosArm64`
- `tvosX64`
- `tvosSimulatorArm64`

²: WebSockets are currently not supported on `js`

## Getting started

If you are new to GraphQL, check out [the tutorial](/tutorial/00-introduction/) that will guide you through building an Android app using Apollo, Kotlin and coroutines.

If you'd like to add Apollo Kotlin to an existing project, follow these steps:

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("com.apollographql.apollo3").version("x.y.z")
}
```

Add the runtime dependency:

```kotlin
dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime:x.y.z")
}
```

Set the package name to use for the generated models:

```kotlin
apollo {
  packageName.set("com.example")
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
* Factor common patterns using [fragments](./essentials/inline-fragments)

## Requirements

Some platforms have specific requirements:

* Android API level 21+
* JDK 8+
* iOS 13+

For building, it requires:

* Gradle 5.6
* Kotlin 1.4+

## IntelliJ Plugin

The [JS Graphql IntelliJ Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) provides auto-completion, error highlighting, and go-to-definition functionality for your `.graphql` files. You can create a [`.graphqlconfig`](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files) file to use GraphQL scratch files to work with your schema outside product code (such as to write temporary queries to test resolvers).

## Releases

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo3/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo3/)

Check the [changelog](https://github.com/apollographql/apollo-android/releases) for the release history.

Releases are hosted on [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo3/). The plugin is additionally hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.apollographql.apollo3)


```kotlin
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

## Contributing

If you'd like to contribute, please see [Contributing.md](https://github.com/apollographql/apollo-android/blob/main/Contributing.md).

## Additional resources

- [A journey to Kotlin multiplatform](https://www.youtube.com/watch?v=GN6LHrqyimI): how the project was moved to Kotlin multiplatform, talk given at Kotliners in June 2020.
- [#125, Fragmented Podcast](http://fragmentedpodcast.com/episodes/125/): Why's and How's about Apollo Kotlin and the entire journey.
- [GraphQL.org](http://graphql.org) for an introduction and reference to GraphQL itself.
- [apollographql.com](http://www.apollographql.com/) to learn about Apollo open-source and commercial tools.
- [The Apollo blog](https://www.apollographql.com/blog/) for long-form articles about GraphQL, feature announcements for Apollo, and guest articles from the community.
- [The Apollo Twitter account](https://twitter.com/apollographql) for in-the-moment news.
