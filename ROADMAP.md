# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on September 9th, 2020. For a more detailed and certainly more up-to-date view, check the project's [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open).

## `main` branch (versions 2.x)

`main` is the stable branch. Non API-breaking improvements and bugfixes will land there. Releases are typically made every 2-3 weeks. What's coming:

- **Multi-modules support** ([#1973](https://github.com/apollographql/apollo-android/issues/1973)): Big codebase use Gradle modules for better separation of concern as well as to improve build speed. Currently, the recommendation is to put all the GraphQL queries in a common module but that scales poorly. Multi-modules support will allow defining operations in the module they are used as well as sharing GraphQL Fragments between multiple modules.  
    
- **Gradle configuration cache** ([#2524](https://github.com/apollographql/apollo-android/issues/2524)): Gradle 6.6 introduced [configuration caching](https://docs.gradle.org/current/userguide/configuration_cache.html) as an incubating feature. We want to support that to make builds faster.

- **General bug-bashing**: We'll be dealing with any new issues and small feature requests as they come up.


## `dev-3x` branch (versions 3.x)

`dev-3x` is the branch for the next major version of Apollo Android. Most of the changes listed below will require breaking changes and will land in `dev-3x`. The current target is to get a `3.0.0-alpha1` by the end of 2020 including:

- **Fragments as interfaces** ([#1854](https://github.com/apollographql/apollo-android/issues/1854)): As of today, GraphQL fragments are generated as separate classes. While this works well, accessing fragments is verbose: `hero.fragments.humanDetails?.homePlanet`. Having fragments generated as interface will make the code more concise: `(hero as? Human)?.homePlanet`, possibly `hero.homePlanet` if the type condition is always verified.

- **Streaming Json parser** ([#2523](https://github.com/apollographql/apollo-android/issues/2523)): Benchmarks have shown that parsing Json with Apollo Android is slower than with [Moshi](https://github.com/square/moshi). Part of the explanation is that, as of now, we parse in two steps. First from the json to a map and then from the map to the generated models. This is required with the current fragment's implementation. Reading fragment requires being able to "rewind" the stream to read fields that are defined multiple times. With fragments as interfaces, we could switch to a streaming parser and get some nice performance improvements.

- **Kotlin multiplatform normalized cache** ([#2222](https://github.com/apollographql/apollo-android/issues/2222)): The Kotlin multiplatform runtime is working but is still missing features compared to the JVM runtime. The normalized cache is one of them.

- **Improved normalized cache APIs** ([#2331](https://github.com/apollographql/apollo-android/issues/2331)): It's been historically difficult to handle cases like pagination with the current normalized cache. We have also had a lot of questions around the usage of `CacheKeyResolver` and data expiration, garbage collection. Making the normalized cache multiplatform is a good opportunity to improve these APIs. 

- **General API grooming**: As part of the major release, remove some deprecated APIs and streamline some complex APIs such as the custom scalars and the Android variants handling in the Gradle plugin.     
