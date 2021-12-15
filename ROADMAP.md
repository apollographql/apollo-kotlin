# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Kotlin in the short and longer term.

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Kotlin and what you'd like to see in the future.

This document was last updated on December 15th, 2021.

## `main` branch

Apollo Kotlin 3 is the latest version of Apollo Kotlin. It's 100% written in Kotlin and brings [a ton of new APIs and features](https://github.com/apollographql/apollo-kotlin/releases/tag/v3.0.0). 

The next things to get worked on are:

### 3.0 Feedbacks

As Apollo Kotlin 3 is relatively young, there will be use cases and issues that are not addressed yet. Our priority will be to make sure the feedbacks are addressed and any bug fixed rapidly. 

### Cache improvements

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction.

### Test server

Apollo Kotlin 3 introduces [test builders](https://www.apollographql.com/docs/kotlin/advanced/test-builders/) as a new type-safe way to build test data. We can go a step further and build an all-in-one solution that includes a GraphQL aware server.

### Make Apollo Kotlin even more multiplatform

Apollo Kotlin 3 is multiplatform first with runtime and cache support for the JVM, iOS, macOS and JS. We can do more to make the library even more easy to use:

- HMPP support
- Trying out and adopting the new memory model
- More targets like linux/mingw/etc..

## `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.



