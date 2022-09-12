# 🔮 Apollo Kotlin Roadmap

**Last updated: Sept 2022**

For up to date release notes, refer to the project [Change Log](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## Upcoming Releases

### Release TBD

- This release will focus on providing better Java support in Apollo Kotlin.  See Github [Java Support Release TBD](https://github.com/apollographql/apollo-kotlin/milestone/25) for more details.

## Longer Term

### Cache improvements

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction. Follow [#2331](https://github.com/apollographql/apollo-kotlin/issues/2331) for a high level overview. The current focus is on cache control and expiration. You can read more in [the cache control design document](https://github.com/apollographql/apollo-kotlin/pull/4009).

### Rel 1.0 Android Studio Plugin

Currently there is no plugin for Android Studio for Apollo GraphQL. We'd like to offer better support for Android developers who wish to use our Kotlin Client.

### Better support for Jetpack Compose

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin. We'd like to offer better support for Android developers who wish to use Jetpack Compose with our Kotlin Client.

### `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.
