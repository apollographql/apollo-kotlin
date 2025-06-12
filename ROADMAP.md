# 🔮 Apollo Kotlin Roadmap

**Last updated: 2025-06-12**

For up to date release notes, refer to the project [Changelog](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> **Please note:** This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## `main` is now v5

All active feature development is now being done for `5.x` releases on the `main` branch.  Critical bugfixes and security patches will land in version 4 on the `release-4.x` branch.

v5 focuses on removing deprecated symbols to keep the codebase clean as well as incremental additions:
* GraphQL spec tracking: support for [default values coercion](https://github.com/graphql/graphql-spec/pull/793/), [schema coordinates](https://github.com/graphql/graphql-spec/pull/794/), [fragment arguments](https://github.com/graphql/graphql-spec/pull/1081), [`@stream`](https://github.com/graphql/graphql-spec/pull/742), ...
* Testing improvements: [data builders in the test source set](https://github.com/apollographql/apollo-kotlin/issues/5257), [strict mode](https://github.com/apollographql/apollo-kotlin/issues/3344), ...
* More KMP targets: linux, wasm
* ...

The scope will be refined as the release date approaches. There should overall be no big bang. We aim for ABI compatibility for all symbols except those that were deprecated in v4 (and have been removed), `@ApolloExperimental` symbols and artifacts used at build time (`apollo-gradle-plugin`, `apollo-compiler`, `apollo-tooling`).

With `apollo-kotlin` being more and more stable, most of the work is now happening in [Apollo Galaxy repos](https://www.apollographql.com/docs/kotlin/advanced/galaxy), most notably the [normalized cache](https://github.com/apollographql/apollo-kotlin-normalized-cache). 


## Jetpack Compose extensions 

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin.  We are experimenting with a few different approaches for supporting Compose in the Apollo Kotlin library.  Our 3.8.0 release introduced an experimental API for use with Compose but gathered little feedback. We're planning to revisit this with more ambitious goals in terms of fragments colocation, error boundaries and more generally integration with the UI framework.

This pattern is encouraged by [Relay](https://relay.dev/docs/tutorial/fragments-1/) and [Apollo Client (TypeScript)](https://www.apollographql.com/blog/optimizing-data-fetching-with-apollo-client-leveraging-usefragment-and-colocated-fragments) and may prove to be valuable to developers using Compose.

## IntelliJ plugin usability

We are planning to remove some of the limitations of the current plugin in order to make it easier to work with (by simplifying configuration, especially for server vs client use cases) as well as consume less resources (by skipping the Gradle daemon). This work is exploratory and will also unlock future improvements for the plugin such as adopting new GraphQL features and directives faster.

## [Cache improvements](https://github.com/apollographql/apollo-kotlin/issues/2331) (on pause, feedback needed 🙏)

Apollo Normalized Cache v1 alphas [are available now](https://github.com/apollographql/apollo-kotlin-normalized-cache/releases) and contain lots of new features like [Cache Control](https://apollographql.github.io/apollo-kotlin-normalized-cache/cache-control.html), [garbage collection](https://apollographql.github.io/apollo-kotlin-normalized-cache/garbage-collection.html), TTL, [partial cache results](https://github.com/apollographql/apollo-kotlin-normalized-cache/issues/57), better performance and more...

We encourage you to try it out with the (important) caveat that the binary format might still change (your persistent cache might be lost when upgrading to a newer alpha versions, memory cache isn't impacted).  Your feedback is greatly appreciated and helps us ship a stable version faster.

## [Apollo Kotlin Faker](https://github.com/apollographql/apollo-kotlin-faker) (on pause, feedback needed 🙏)

Apollo Kotlin Faker [is now available](https://github.com/apollographql/apollo-kotlin-faker/releases) for rapid schema-driven testing, a pattern that we have seen used with success in other Apollo projects.  We encourage you to try it out.  Your feedback is greatly appreciated and helps us ship a stable version faster.

