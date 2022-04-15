# Benchmark

This is a standalone Android Studio project to benchmark performance of the Json parser. While the root `apollo-android` project can be opened with IntelliJ, this one requires Android Studio.

It's not a composite build so it's easy to swap the Apollo version but it reuses the dependencies from the main project and declares OSS Snapshots and mavenLocal() as fallback repositories (in that order) so you can use `publishToMavenLocal` and snapshots easily.
Also because the Android Plugin and the Kotlin multiplatform one do not play super nice together so it's easier to make it a separate project.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will print the results in the `Run` window of Android Studio

## Current results:

Run on a Pixel 3 XL. Feel free to update/commit new results and we can get the history using `git annotate`

```
   15,155,158 ns Benchmark.apollo
   13,326,669 ns Benchmark.moshi
   19,175,002 ns Benchmark.apolloBatchCacheMemory
   44,868,183 ns Benchmark.apolloParseAndNormalize
  134,730,430 ns Benchmark.apolloBatchCacheSql
```

