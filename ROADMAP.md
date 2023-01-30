# 🔮 Apollo Kotlin Roadmap

**Last updated: 2023-01-30**

For up to date release notes, refer to the project [Changelog](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## Version 3 releases

`3.7` is the last planned minor release under version 3.  All active feature development is now being done for the `4.0.0` release on the `main` branch.  Version 3 is still actively maintained and we will continue to release `3.7.x` patches on the `release-3.x` branch.

## [4.0](https://github.com/apollographql/apollo-kotlin/milestone/29)

_Approximate Dates: 2023-04-27 (Alpha), 2023-07-31 (Beta), 2023-09-26 (GA)_

Our next major release is currently in active development. You can review this [umbrella issue](https://github.com/apollographql/apollo-kotlin/issues/4171) which identifies the planned changes in `4.0`.  Here's a high-level overview of what to expect:

- [IntelliJ / Android Studio plugin](https://github.com/apollographql/apollo-kotlin/issues?q=is%3Aissue+is%3Aopen+plugin+label%3A%22%F0%9F%90%99+IJ%2FAS+plugin%22)
- [Better Java support](https://github.com/apollographql/apollo-kotlin/milestone/25)
- Annotation processing for custom scalars
- Client-controlled nullability
- New tutorial with Jetpack Compose

## Better support for Jetpack Compose

_Approximate Date: TBD_

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin.  We plan to offer better integrations with Compose in a 4.x minor release.

## Cache improvements

_Approximate Date: TBD_

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction. Follow [#2331](https://github.com/apollographql/apollo-kotlin/issues/2331) for a high level overview. The current focus is on cache control and expiration. You can read more in [the cache control design document](https://github.com/apollographql/apollo-kotlin/pull/4009).

### `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.
