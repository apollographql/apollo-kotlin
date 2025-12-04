# 🔮 Apollo Kotlin Ecosystem Roadmap

**Last updated: 2025-12-04**

For up-to-date release notes, refer to the project [Changelog](https://github.com/apollographql/apollo-kotlin/blob/main/CHANGELOG.md).

> [!NOTE]
> This is an approximation of **larger effort** work planned for the next 6 - 12 months. It does not cover all new functionality that will be added, and nothing here is set in stone. Also note that each of these releases, and several patch releases in-between, will include bug fixes (based on issue triaging) and community submitted PR's.

## ✋ Community feedback & prioritization

- Please report feature requests or bugs as a new [issue](https://github.com/apollographql/apollo-kotlin/issues/new/choose).
- If you already see an issue that interests you please add a 👍 or a comment so we can measure community interest.

---

## Apollo Kotlin

`main` is now v5.

All active feature development is now being done for `5.x` releases on the `main` branch.  Critical bugfixes and security patches will land in version 4 on the `release-4.x` branch.

The scope will be refined as the release date approaches.

We aim for ABI compatibility for all symbols except those that were deprecated in v4 (and have been removed), `@ApolloExperimental` symbols and artifacts used at build time (`apollo-gradle-plugin`, `apollo-compiler`, `apollo-tooling`).

### Incremental delivery: protocol updates and `@stream` support

v5.0.0-alpha.3 supports [incremental:v0.1](https://specs.apollo.dev/incremental/v0.1/) (`@defer`) and [incremental:v0.2](https://specs.apollo.dev/incremental/v0.2/)) (both `@defer` and `@stream`).

The [GraphQL Specification RFC](https://github.com/graphql/graphql-spec/pull/1110) is not merged yet. We continue monitoring the RFC and will adapt the implementation if needed.

### Other GraphQL spec tracking items

We plan to implement these GraphQL specification RFCs:

- [x] [Default values coercion](https://github.com/graphql/graphql-spec/pull/793/)
- [x] [Schema coordinates](https://github.com/graphql/graphql-spec/pull/794/)
- [ ] [Fragment arguments](https://github.com/graphql/graphql-spec/pull/1081)
- [ ] [Service capabilities](https://github.com/graphql/graphql-spec/pull/1163)
- [ ] [Directives on directive definitions](https://github.com/graphql/graphql-spec/pull/567)

### Testing improvements:

We have recently shipped these features, please try them out and give us feedback :)

- [x] [Data builders in the test source set](https://github.com/apollographql/apollo-kotlin/issues/5257)
- [x] [Strict mode](https://github.com/apollographql/apollo-kotlin/issues/3344)
- [ ] [Test server generator (CLI)](https://github.com/apollographql/apollo-kotlin-cli/issues/25)

## [Cache improvements](https://github.com/apollographql/apollo-kotlin/issues/2331)

The new Apollo Normalized Cache v1 beta [is available now](https://github.com/apollographql/apollo-kotlin-normalized-cache/releases) and contain lots of new features like [Cache Control](https://apollographql.github.io/apollo-kotlin-normalized-cache/cache-control.html), [garbage collection](https://apollographql.github.io/apollo-kotlin-normalized-cache/garbage-collection.html), [partial cache results](https://github.com/apollographql/apollo-kotlin-normalized-cache/issues/57), better performance and more...

We encourage you to try it out with the (important) caveat that the binary format might still change (your persistent cache might be lost when upgrading to a newer prerelease version, memory cache isn't impacted).  Your feedback is greatly appreciated and helps us ship a stable version faster.

## Apollo Kotlin Compose

[Jetpack Compose](https://developer.android.com/jetpack/compose) is a declarative UI framework for building Android UIs written in Kotlin.  [Apollo Kotlin Compose](https://github.com/apollographql/apollo-kotlin-compose) is an experimental framework that, along with a [special compiler plugin](https://github.com/apollographql/apollo-kotlin-compiler-plugin), provides APIs for Compose users with the Apollo Kotlin library.  This new framework has ambitious goals in terms of fragments colocation, error boundaries and more generally integration with the UI framework. Check out [this GraphQLConf video](https://www.youtube.com/watch?v=94Nz2B6ETD8) about it!

This pattern is encouraged by [Relay](https://relay.dev/docs/tutorial/fragments-1/) and [Apollo Client (TypeScript)](https://www.apollographql.com/blog/optimizing-data-fetching-with-apollo-client-leveraging-usefragment-and-colocated-fragments) and may prove to be valuable to developers using Compose.

## [IntelliJ plugin usability](https://github.com/apollographql/apollo-intellij-plugin)

IJ Plugin version v5 is released 🎉 
Highlights includes:
* Support for `@link`.
* No-Gradle mode: if you are using Apollo 5, the IJ plugin can now invoke the Apollo compiler directly without needing a full Gradle daemon.

See the [release notes](https://github.com/apollographql/apollo-intellij-plugin/releases/tag/v5.0.0) for details.

## [Apollo Kotlin Faker](https://github.com/apollographql/apollo-kotlin-faker) (on pause, feedback needed 🙏)

Apollo Kotlin Faker [is now available](https://github.com/apollographql/apollo-kotlin-faker/releases) for rapid schema-driven testing, a pattern that we have seen used with success in other Apollo projects.  We encourage you to try it out.  Your feedback is greatly appreciated and helps us ship a stable version faster.
