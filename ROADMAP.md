# 🔮 Apollo Kotlin Roadmap

**Last updated: 2024-04-19**

For up to date release notes, refer to the project [Changelog](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## Version 3 releases

`3.8` is the last planned minor release under version 3.  All active feature development is now being done for the `4.0.0` release on the `main` branch.  Version 3 is still actively maintained and we will continue to release `3.8.x` patches on the `release-3.x` branch.

## [4.0](https://github.com/apollographql/apollo-kotlin/milestone/29)

_Approximate GA Date: June 2024 (depends on Kotlin 2.0 GA date)_

Our next major release is currently in the beta stage. Expect new beta to be released as we make progress towards a GA release candidate. This major version removes some deprecated APIs but will otherwise contain mostly incremental changes and most of the API will stay compatible.  We plan to do a GA release after Kotlin 2.0.0 is generally available.

Here's a high-level overview of the feature set:

- [IntelliJ / Android Studio plugin](https://github.com/apollographql/apollo-kotlin/issues?q=is%3Aissue+is%3Aopen+plugin+label%3A%22%F0%9F%90%99+IJ%2FAS+plugin%22)
- [Java runtime](https://github.com/apollographql/apollo-kotlin/milestone/25)
- Error handling changes ([RFC](https://github.com/apollographql/apollo-kotlin/issues/4711))
    - Move exceptions to `ApolloResponse`
    - Add `@catch` directive ([RFC](https://github.com/apollographql/apollo-kotlin/issues/5337))
    - Add `@semanticNonNull` directive
- [API tweaks and cleanups](https://github.com/apollographql/apollo-kotlin/issues/4171)
- Support `@oneOf` for Input Objects
- Support Wasm JS target
- [Low-level compiler APIs](https://github.com/apollographql/apollo-kotlin/issues/5415)
- Network-awareness APIs for integration with Android and iOS connectivity managers
- Retry functionality for HTTP Multipart and WebSockets protocols

## Jetpack Compose extensions

_This is currently available as an experimental feature.  We will release a stable version after getting sufficient user feedback_

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin.  We are experimenting with a few different approaches for supporting Compose in the Apollo Kotlin library.  Our 3.8.0 release introduced an experimental API for use with Compose, please do try it out and give us feedback!

## Cache improvements

_Approximate Date: TBD_

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction. Follow [#2331](https://github.com/apollographql/apollo-kotlin/issues/2331) for a high level overview. The current focus is on cache control and expiration. You can read more in [the cache control design document](https://github.com/apollographql/apollo-kotlin/pull/4009).

## Future feature releases

- Annotation processing for custom scalars
- Stable Jetpack Compose extensions - user feedback is critical here, please do try out the experimental extensions and let us know what's working and what could be improved!

### `release-2.x` branch

`release-2.x` is the Apollo Android `2.x` branch. Major security issues or fixes will get new releases.
