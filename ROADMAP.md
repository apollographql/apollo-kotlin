# 🔮 Apollo Kotlin Roadmap

**Last updated: April 2022**

For up to date release notes, refer to the project [Change Log](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

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

### `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.
