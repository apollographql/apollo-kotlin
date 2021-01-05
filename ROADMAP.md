# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on January 5th, 2021. For a more detailed and up-to-date view, you can check the project's [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open).

## `main` branch (versions 2.x)

`main` is the stable branch. Non breaking improvements and bugfixes land here. This branch is actively maintained and battle-tested. 

While working on 2.x, we found a few limitations that could not be fixed without major breaking changes so we started developping `dev-3.x` in parallel. Once `dev-3.x` reaches alpha, it will be merged into `main`

## `dev-3x` branch (versions 3.x)

`dev-3x` is the branch for the next major version of Apollo Android where new developments happen. Apollo 3.0.0 will be:

* **Kotlin-first**, generating Kotlin models by default and exposing coroutines APIs
* **Modular**, making it easy to change the transport, the cache implementation or using the generated models directly. 
* **Fast**, with optimized json parsing speed to make your UIs even more reactive

Generating Fragments as interfaces has proved to be quite challenging with a lot of edge cases so that's taking some time but we're hoping to release an alpha in Q1 2021. The `3.0.0-SNAPSHOTS` [are available on Sonatype snapshots](https://github.com/apollographql/apollo-android#snapshots) if you want to try it out. The reason it's not alpha yet is that it still misses a few features compared to 2.x. The full list of issues can be found in the [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open). The most important ones are:

- **Fragments as interfaces** ([#1854](https://github.com/apollographql/apollo-android/issues/1854)): In 2.x, GraphQL fragments are generated as separate classes. While this works well, accessing fragments is verbose: hero.fragments.humanDetails?.homePlanet. Having fragments generated as interfaces will make the code more concise: `hero.humanDetails()?.homePlanet`. Code generation is now mostly working 🎉. It's a considerable change from 2.x so it will requires a lot of testing. 

- **Streaming Json parser** ([#2523](https://github.com/apollographql/apollo-android/issues/2523)): Fragments as interfaces enable to stream the json response, i.e. read generating models directly from the Json parser instead of parsing to a Map and then to the generated models. This is now working in simple cases and showed a ~60% performance improvement in Json parsing. It still needs to be enabled in all cases, especially when normalization is involved.

- **Kotlin multiplatform normalized cache** ([#2636](https://github.com/apollographql/apollo-android/issues/2636)): The multiplatform cache infrastructure is now working with a Cache interceptor, SQLLite and in-memory cache. It's still missing watchers, custom fetchers, imperative store API and a few other things.

- **General API grooming**: As part of the major release, remove some deprecated APIs and streamline some complex APIs. Custom scalars (PR #2486) and the Android variants handling in the Gradle plugin (PR #2668) have been tweaked but new opportunities to cleanup the API will certainly show up. 

- **Improved normalized cache APIs** ([#2331](https://github.com/apollographql/apollo-android/issues/2331)): It's been historically difficult to handle cases like pagination with the current normalized cache. We have also had a lot of questions around the usage of `CacheKeyResolver` and data expiration, garbage collection. Making the normalized cache multiplatform is a good opportunity to improve these APIs. This is the next big thing to tackle.




