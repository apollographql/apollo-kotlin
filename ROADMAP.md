# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on April 7th, 2021. For a more detailed and up-to-date view, you can check the project's [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open).

## `2.x` branch 

`main` is the stable `2.x` branch. Non breaking improvements and bugfixes land here. This branch is battle-tested and actively maintained. The next release will include the Gradle plugin as a fatjar to workaround some issues with Gradle classloaders (https://github.com/apollographql/apollo-android/pull/3023) as well as other bugfixes.

While working on 2.x, we found a few limitations that could not be fixed without major breaking changes so we started developping `dev-3.x` in parallel. Once `dev-3.x` reaches alpha, it will be merged into `main`

## `3.0.0-alpha1`

`dev-3.x` is the branch for the next major version of Apollo Android where new developments happen. A lot of progress has been done on performance ([#2895](https://github.com/apollographql/apollo-android/issues/2895), [#2651](https://github.com/apollographql/apollo-android/issues/2651)), multiplatform ([#2878](https://github.com/apollographql/apollo-android/issues/2878), [#2983](https://github.com/apollographql/apollo-android/issues/2983)) and codegen ([#1854](https://github.com/apollographql/apollo-android/issues/1854)). Developer preview builds are published on maven central under the [`com.apollographql.apollo3`](https://repo1.maven.org/maven2/com/apollographql/apollo3/) group, feel free to try them out! 

Before going `alpha`, the `dev-3.x` branch is still missing:

- **Multiplatform watchers, APQs and optimistic updates** ([#2904](https://github.com/apollographql/apollo-android/issues/2904)): The multiplatform cache infrastructure is now working with a Cache interceptor, SQLLite and in-memory cache. It's still missing watchers, custom fetchers, imperative store API and a few other things.

- **Declarative cache** ([#2331](https://github.com/apollographql/apollo-android/issues/2331)): It's been historically difficult to handle cases like pagination with the current normalized cache. We have also had a lot of questions around the usage of `CacheKeyResolver` and data expiration, garbage collection. Making the normalized cache multiplatform is a good opportunity to improve these APIs. This is the next big thing to tackle.

## `3.0.0-alpha2`

After `3.0.0-alpha1`, `3.0.0-alpha2` will focus on bringing incremental improvements:

- **SQL Cache expiration** ([#2331](https://github.com/apollographql/apollo-android/issues/2331))

- **Testing APIs** ([#3028](https://github.com/apollographql/apollo-android/issues/3028))



