---
title: Introduction
description: A strongly-typed, caching GraphQL client for Android and Kotlin multiplatform, written in Kotlin
---

[Apollo Android](https://github.com/apollographql/apollo-android) is a strongly-typed, caching GraphQL client for Android and Kotlin multiplatform apps written in Kotlin. Apollo Android works well with Android, but you can use it in any Java/Kotlin app.

It allows you to execute queries and mutations against a GraphQL server and returns results as query-specific Kotlin types. This means you don't have to deal with parsing JSON, or passing around Maps and making clients cast values to the right type manually. You also don't have to write model types yourself, because these are generated from the GraphQL definitions your UI uses.

As the generated types are query-specific, you're only able to access data you actually specify as part of a query. If you don't ask for a field in a particular query, you won't be able to access the corresponding property on the returned data structure.

## Features

* Kotlin code generation
* Queries, Mutations and Subscriptions
* Reflection-free parsing of responses
* Normalized cache
* HTTP cache
* File uploads
* Custom scalar types

## Requirements

Apollo Android runs on the following platforms:

* Android API level 15+
* JDK 8+
* iOS 13+

For building, it requires:

* Gradle 5.6
* Kotlin 1.4+

## Getting started

[Get started](https://www.apollographql.com/docs/android/get-started) shows how to add Apollo Android to a Kotlin project.

## Related platforms

[Apollo iOS](https://github.com/apollographql/apollo-ios) is a GraphQL client for native iOS apps written in Swift.

Apollo Client for JavaScript's [React integration](https://apollographql.com/docs/react) works with [React Native](https://facebook.github.io/react-native/) on both iOS and Android.

## Other resources

- [GraphQL.org](http://graphql.org) for an introduction and reference to the GraphQL itself, partially written and maintained by the Apollo team.
- [Our website](http://www.apollographql.com/) to learn about Apollo open-source and commercial tools.
- [Our blog](https://www.apollographql.com/blog/) for long-form articles about GraphQL, feature announcements for Apollo, and guest articles from the community.
- [Our Twitter](https://twitter.com/apollographql) for in-the-moment news.
