
# Apollo Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/main/LICENSE)
[![Slack](https://img.shields.io/static/v1?label=kotlinlang&message=apollo-android&color=15a2f5&logo=slack)](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)
[![Join the community](https://img.shields.io/discourse/status?label=Join%20the%20community&server=https%3A%2F%2Fcommunity.apollographql.com)](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
[![CI](https://github.com/apollographql/apollo-android/workflows/CI/badge.svg)](https://github.com/apollographql/apollo-android/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo/)
[![OSS Snapshots](https://img.shields.io/nexus/s/com.apollographql.apollo/apollo-api?server=https%3A%2F%2Foss.sonatype.org&label=oss-snapshots)](https://oss.sonatype.org/content/repositories/snapshots/com/apollographql/apollo/)

> **Apollo Android 3 is available in alpha.** It is still in active development, and we'd love for folks to test it out. [See the v3 documentation](https://www.apollographql.com/docs/android/v3) and please [report any issues](https://github.com/apollographql/apollo-android/issues/new/choose)!

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
* Reactive bindings for: RxJava2, RxJava3, Coroutines, Reactor and Mutiny

## Getting started

If you are new to GraphQL, check out [the tutorial](https://www.apollographql.com/docs/android/tutorial/00-introduction/) that will guide you through building an Android app using Apollo, Kotlin and coroutines.

If you'd like to add Apollo Android to an existing project:

* [Get started with Kotlin](https://www.apollographql.com/docs/android/essentials/get-started-kotlin) shows how to add Apollo Android to a Kotlin project.
* [Get started with Java](https://www.apollographql.com/docs/android/essentials/get-started-java) shows how to add Apollo Android to a Java project.
* [Get started with Multiplatform (Experimental)](https://www.apollographql.com/docs/android/essentials/get-started-multiplatform) shows how to add Apollo Android to a Multiplatform project. This is still under heavy development and APIs may change without warning.


## Advanced topics

Check [the project website](https://www.apollographql.com/docs/android/) for in depth documentation about [caching](https://www.apollographql.com/docs/android/essentials/caching/), [plugin configuration](https://www.apollographql.com/docs/android/essentials/plugin-configuration/), [android](https://www.apollographql.com/docs/android/advanced/android/), [file upload](https://www.apollographql.com/docs/android/advanced/file-upload/), [coroutines](https://www.apollographql.com/docs/android/advanced/coroutines/), [rxjava2](https://www.apollographql.com/docs/android/advanced/rxjava2/), [rxjava3](https://www.apollographql.com/docs/android/advanced/rxjava3/), [reactor](https://www.apollographql.com/docs/android/advanced/reactor/), [mutiny](https://www.apollographql.com/docs/android/advanced/mutiny/), [persisted queries](https://www.apollographql.com/docs/android/advanced/persisted-queries/), [no runtime](https://www.apollographql.com/docs/android/advanced/no-runtime/), [migrations](https://www.apollographql.com/docs/android/essentials/migration/) and much more...

## IntelliJ Plugin

The [JS Graphql IntelliJ Plugin](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/) provides auto-completion, error highlighting, and go-to-definition functionality for your `.graphql` files. You can create a [`.graphqlconfig`](https://jimkyndemeyer.github.io/js-graphql-intellij-plugin/docs/developer-guide#working-with-graphql-endpoints-and-scratch-files) file to use GraphQL scratch files to work with your schema outside product code (such as to write temporary queries to test resolvers).

## Releases

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo/apollo-api)](https://repo1.maven.org/maven2/com/apollographql/apollo/)

Check the [changelog](https://github.com/apollographql/apollo-android/releases) for the release history.

Releases are hosted on [Jcenter](https://jcenter.bintray.com/com/apollographql/apollo/) and [Maven Central](https://repo1.maven.org/maven2/com/apollographql/apollo/). The plugin is additionally hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.apollographql.apollo)


```groovy:title=build.gradle.kts
plugins {
  id("com.apollographql.apollo").version("x.y.z")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime:x.y.z")

  // optional: if you want to use the normalized cache
  implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite:x.y.z")
  // optional: for coroutines support
  implementation("com.apollographql.apollo:apollo-coroutines-support:x.y.z")
  // optional: for Mutiny support
  implementation("com.apollographql.apollo:apollo-mutiny-support:x.y.z")
  // optional: for Reactor support
  implementation("com.apollographql.apollo:apollo-reactor-support:x.y.z")
  // optional: for RxJava3 support
  implementation("com.apollographql.apollo:apollo-rx3-support:x.y.z")
  // optional: Most of apollo-android does not depend on Android in practice and runs on any JVM or on Kotlin native. apollo-android-support contains a few Android-only helper classes. For an example to send logs to logcat or run callbacks on the main thread.
  implementation("com.apollographql.apollo:apollo-android-support:x.y.z")
  // optional: if you just want the generated models and parsers and write your own HTTP code/cache code, you can remove apollo-runtime
  // and use apollo-api instead
  implementation("com.apollographql.apollo:apollo-api:x.y.z")
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

## Requirements

Apollo Android runs on the following platforms:

* Android API level 15+
* JDK 8+
* iOS 13+

## Contributing

If you'd like to contribute, please see [Contributing.md](https://github.com/apollographql/apollo-android/blob/main/Contributing.md).

## Community integrations

* If you're using the [Maven](https://maven.apache.org/) build tool, https://github.com/aoudiamoncef/apollo-client-maven-plugin is a Maven plugin that calls the Apollo Android compiler to generate your Java/Kotlin sources.

## Additional resources

- [MortyComposeKMM](https://github.com/joreilly/MortyComposeKMM): A Kotlin Multiplatform Github template using Apollo Android, SwiftUI and Jetpack Compose.
- [A journey to Kotlin multiplatform](https://www.youtube.com/watch?v=GN6LHrqyimI): how the project was moved to Kotlin multiplatform, talk given at Kotliners in June 2020.
- [#125, Fragmented Podcast](http://fragmentedpodcast.com/episodes/125/): Why's and How's about Apollo Android and the entire journey.
- [GraphQL.org](http://graphql.org) for an introduction and reference to GraphQL itself.
- [apollographql.com](http://www.apollographql.com/) to learn about Apollo open-source and commercial tools.
- [The Apollo blog](https://www.apollographql.com/blog/) for long-form articles about GraphQL, feature announcements for Apollo, and guest articles from the community.
- [The Apollo Twitter account](https://twitter.com/apollographql) for in-the-moment news.

## Who is Apollo?

[Apollo](https://apollographql.com/) builds open-source software and a graph platform to unify GraphQL across your apps and services. We help you ship faster with:

* [Apollo Studio](https://www.apollographql.com/studio/develop/) – A free, end-to-end platform for managing your GraphQL lifecycle. Track your GraphQL schemas in a hosted registry to create a source of truth for everything in your graph. Studio provides an IDE (Apollo Explorer) so you can explore data, collaborate on queries, observe usage, and safely make schema changes.
* [Apollo Federation](https://www.apollographql.com/apollo-federation) – The industry-standard open architecture for building a distributed graph. Use Apollo’s gateway to compose a unified graph from multiple subgraphs, determine a query plan, and route requests across your services.
* [Apollo Client](https://www.apollographql.com/apollo-client/) – The most popular GraphQL client for the web. Apollo also builds and maintains [Apollo iOS](https://github.com/apollographql/apollo-ios) and [Apollo Android](https://github.com/apollographql/apollo-android).
* [Apollo Server](https://www.apollographql.com/docs/apollo-server/) – A production-ready JavaScript GraphQL server that connects to any microservice, API, or database. Compatible with all popular JavaScript frameworks and deployable in serverless environments.

## Learn how to build with Apollo

Check out the [Odyssey](https://odyssey.apollographql.com/) learning platform, the perfect place to start your GraphQL journey with videos and interactive code challenges. Join the [Apollo Community](https://community.apollographql.com/) to interact with and get technical help from the GraphQL community.
