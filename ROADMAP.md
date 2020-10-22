# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on October 20th, 2020. For a more detailed and up-to-date view, you can check the project's [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open).

## `main` branch (versions 2.x)

`main` is the stable branch. Non breaking improvements and bugfixes land here. This branch is actively maintained and battle-tested. Things that are coming in 2.4.2:

* SDL type extensions ([#2656](https://github.com/apollographql/apollo-android/issues/2656))
* Custom Fragments package names ([#2667](https://github.com/apollographql/apollo-android/issues/2667))
* HTTP cache key bypass ([#2659](https://github.com/apollographql/apollo-android/issues/2659))
* Bugfixes

While working on 2.x, we found a few limitations that could not be fixed without major breaking changes so we started developping `dev-3.x` in parallel. Once `dev-3.x` reaches alpha, it will be merged into `main`

## `dev-3x` branch (versions 3.x)

`dev-3x` is the branch for the next major version of Apollo Android where new developments happen. Apollo 3.0.0 will be:

* **Kotlin-first**, generating Kotlin models by default and exposing coroutines APIs
* **Modular**, making it easy to change the transport, the cache implementation or using the generated models directly. 
* **Fast**, with optimized json parsing speed to make your UIs even more reactive

The current target is to get a `3.0.0-alpha1` by the end of 2020. The full list of issues can be found in the [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open). The most important ones are:

- **Fragments as interfaces** ([#1854](https://github.com/apollographql/apollo-android/issues/1854)): As of today, GraphQL fragments are generated as separate classes. While this works well, accessing fragments is verbose: hero.fragments.humanDetails?.homePlanet. Having fragments generated as interfaces will make the code more concise: `hero.asHuman()?.homePlanet`, even `hero.homePlanet` if the type condition is always verified. Note that due to the massively complex nature of the changes, the Java codegen has been disabled in `dev-3.x` and will have to be mostly rewritten if needed.

- **Streaming Json parser** ([#2523](https://github.com/apollographql/apollo-android/issues/2523)): Benchmarks have shown that parsing Json with Apollo Android is slower than with [Moshi](https://github.com/square/moshi). Part of the explanation is that, as of now, we parse in two steps. First from the json to a map and then from the map to the generated models. This is required with the current fragment's implementation. Reading fragment requires being able to "rewind" the stream to read fields that are defined multiple times. With fragments as interfaces, we could switch to a streaming parser and get some nice performance improvements.

- **Kotlin multiplatform normalized cache** ([#2636](https://github.com/apollographql/apollo-android/issues/2636)): The Kotlin multiplatform runtime is working but is still missing features compared to the JVM runtime. The normalized cache is one of them.

- **Improved normalized cache APIs** ([#2331](https://github.com/apollographql/apollo-android/issues/2331)): It's been historically difficult to handle cases like pagination with the current normalized cache. We have also had a lot of questions around the usage of `CacheKeyResolver` and data expiration, garbage collection. Making the normalized cache multiplatform is a good opportunity to improve these APIs. 

- **General API grooming**: As part of the major release, remove some deprecated APIs and streamline some complex APIs such as the custom scalars ([PR #2486](https://github.com/apollographql/apollo-android/issues/2486)) and the Android variants handling in the Gradle plugin ([PR #2668](https://github.com/apollographql/apollo-android/pull/2668)).     
