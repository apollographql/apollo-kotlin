# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on October 25th, 2021. 

## `2.x` branch 

`main` is the stable `2.x` branch. Non breaking improvements and bugfixes land here. This branch is battle-tested and actively maintained. No new features are planned for this branch and only major security or fixes will get new releases.

## `3.x` branch

**`dev-3.x` is in Beta stage**. It's working, contains [a ton of new features and improvements](https://github.com/apollographql/apollo-android/releases/tag/v3.0.0-alpha01) and a [migration guide](https://www.apollographql.com/docs/android/v3/migration/3.0/). It's not stable yet because the API might change but if you don't mind the occasional APIs updates, **please try it out**. The faster we get feedback, the faster it can reach stable. The current goal is to go stable by the end of 2021.

The next things to get worked on are:

### 3.0 Stabilization and Feedbacks

As we move to a stable 3.0 release our priority will be to make sure the feedbacks are addressed and any bug fixed rapidly. We will also work on streamlining the documentation and migration process. 

### Cache improvements 

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction.

### Make Apollo Android even more multiplatform

Apollo Android 3 is multiplatform first with runtime and cache support for the JVM, iOS, macOS and JS. We can do more to make the library even more easy to use:

- HMPP support
- Trying out and adopting the new memory model
- More targets like linux/mingw/etc..

