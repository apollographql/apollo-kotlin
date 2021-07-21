# Roadmap

This document is meant to give the community some idea of where we're going with Apollo Android in the short and longer term. 

Please open issues or comment/upvote the existing ones for items you'd like to see added here. Feedback is very welcome! We'd love to learn more about how you're using Apollo Android and what you'd like to see in the future.

This document was last updated on July 19th, 2021. For a more detailed and up-to-date view, you can check the project's [milestones](https://github.com/apollographql/apollo-android/milestones?direction=asc&sort=title&state=open).

## `2.x` branch 

`main` is the stable `2.x` branch. Non breaking improvements and bugfixes land here. This branch is battle-tested and actively maintained. No new features are planned for this branch and only major security or fixes will get new releases.

## `3.x` branch

**`dev-3.x` is in Alpha stage**. It's usable and contains [a ton of new features and improvements](https://github.com/apollographql/apollo-android/releases/tag/v3.0.0-alpha01). It's not stable yet because the API might change but if you don't mind the occasional APIs updates, **please try it out**. The faster we get feedback, the faster it can reach stable.

The next things to get worked on are:


### Java Codegen ([#2616](https://github.com/apollographql/apollo-android/issues/2616))

To get a first alpha out of the door, the Java codegen was removed. An upcoming version will introduce it again together with making sure the APIs still work when called from Java

### Testing APIs ([#3028](https://github.com/apollographql/apollo-android/issues/3028))

Generating fake Json data for unit test/integration test is verbose as it requires passing a value for each property and potentially having to go through fragments, etc... A possibility is to record the json with a proxy but this becomes harder to maintain as the schema changes. Having some typesafe way to generate json would be very useful for tests.

### Cache improvements ([2331](https://github.com/apollographql/apollo-android/issues/2331))

The declarative cache makes working with the cache and defining unique object ids easier. We also want to include helpers to handle with common cases like pagination, garbage collection and eviction.

