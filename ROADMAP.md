# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Kotlin in the short and longer term.

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Kotlin and what you'd like to see in the future.

This document was last updated on April 22nd, 2022.

### Community Feedback & bug bashing

We want to make sure the library fits your use cases and welcome all kind of feedback. Our priority will be to make sure the feedback are addressed and any bug fixed rapidly. 

### Cache improvements

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction. Follow [#2331](https://github.com/apollographql/apollo-kotlin/issues/2331) for a high level overview. The current focus is on cache control and expiration. You can read more in [the cache control design document](https://github.com/apollographql/apollo-kotlin/pull/4009).

### Keep up to date with the spec

The GraphQL spec continues to evolve and we want to keep track of new proposals like [`@defer`](https://github.com/graphql/graphql-wg/blob/main/rfcs/DeferStream.md) or [Client Control Nullability](https://github.com/graphql/graphql-spec/issues/867)

### Test APIs

Apollo Kotlin 3 introduces [test builders](https://www.apollographql.com/docs/kotlin/testing/test-builders/) as a new type-safe way to build test data. While this is working, it generates a lot of code, making it hard to include in main source sets. This can be problematic in some cases like Jetpack Compose `@Preview` for an example. We are investigating generating schema-based test builders for simpler and more lightweight APIs.

### Hierarchical Multi Platform Projects (HMPP)

Add support for [HMPP](https://kotlinlang.org/docs/multiplatform-share-on-platforms.html)

### New Kotlin native memory model

As the new [memory model](https://blog.jetbrains.com/kotlin/2021/08/try-the-new-kotlin-native-memory-manager-development-preview/) is becoming the default, adapt Apollo Kotlin to use it.

## `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.



