# Benchmark

This is a standalone Android Studio project to benchmark performance of the Json parser. While the root `apollo-android` project can be opened with IntelliJ, this one requires Android Studio.

It's not a composite build so it's easy to swap the Apollo version but it reuses the dependencies from the main project and declares OSS Snapshots and mavenLocal() as fallback repositories (in that order) so you can use `publishToMavenLocal` and snapshots easily.
Also because the Android Plugin and the Kotlin multiplatform one do not play super nice together so it's easier to make it a separate project.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will print the results in the `Run` window of Android Studio

## Current results:

Run on a Pixel 3 XL. Feel free to update/commit new results and we can get the history using `git annotate`

```
benchmark:    13,157,189 ns Benchmark.moshi
benchmark:    14,255,783 ns Benchmark.apollo
benchmark:    21,737,450 ns Benchmark.apolloParseAndNormalize
benchmark:     4,725,625 ns Benchmark.apolloReadCacheMemory
benchmark:     6,794,531 ns Benchmark.apolloBatchCacheMemory
benchmark:   229,286,897 ns Benchmark.apolloReadCacheSql
benchmark:    72,140,632 ns Benchmark.apolloBatchCacheSql
```

