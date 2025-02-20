# 🔮 Apollo Kotlin Roadmap

**Last updated: 2025-02-20**

For up to date release notes, refer to the project [Changelog](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## [Cache improvements](https://github.com/apollographql/apollo-kotlin/issues/2331)

The declarative cache makes working with the cache and defining unique object ids easier.  We also want to include helpers to handle with common cases like pagination, garbage collection and eviction. Follow [#2331](https://github.com/apollographql/apollo-kotlin/issues/2331) for a high level overview.  

* Cache control is now available ([doc](https://apollographql.github.io/apollo-kotlin-normalized-cache-incubating/cache-control.html)) 🎉.  
* A [first implementation of garbage collection](https://github.com/apollographql/apollo-kotlin-normalized-cache-incubating/pull/69) is also available ([doc](https://apollographql.github.io/apollo-kotlin-normalized-cache-incubating/garbage-collection.html)).
* [Partial cache results](https://github.com/apollographql/apollo-kotlin-normalized-cache-incubating/issues/57) is also available.

Using the incubating cache, early results show a speed improvement. This is to be confirmed on real life use cases. As always, your feedback is greatly appreciated and helps us moving the artifact out of experimental state faster.

## [Testing utilities](https://github.com/apollographql/apollo-kotlin/issues/6076)

The community has given some consistent feedback around testing and data builders in particular.  We are in the process of organizing this feedback into actionable workstreams and will update this section of the Roadmap and the relevant Issues as details emerge.

## Jetpack Compose extensions

_This is currently available as an experimental feature.  We will release a stable version after getting sufficient user feedback_

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin.  We are experimenting with a few different approaches for supporting Compose in the Apollo Kotlin library.  Our 3.8.0 release introduced an experimental API for use with Compose, please do try it out and give us feedback!

## Future feature releases 

- Better support for inline value classes.
- Stable Jetpack Compose extensions - user feedback is critical here, please do try out the experimental extensions and let us know what's working and what could be improved!

## Version 3 releases

All active feature development is now being done for `4.x` releases on the `main` branch.  Critical bugfixes and security patches will land in version 3 via `3.8.x` on the `release-3.x` branch.
