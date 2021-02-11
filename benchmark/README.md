# Benchmark

This is a standalone Android Studio project to benchmark performance of the Json parser. While the root `apollo-android` project can be opened with IntelliJ, this one requires Android Studio.

It's not included in the composite build so it's easy to swap the Apollo version but it reuses the dependencies from the main project and declares OSS Snapshots and mavenLocal() as fallback repositories (in that order) so you can use `publishToMavenLocal` and snapshots easily.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will print the results in the `Run` window of Android Studio

## Current results:

Run on a Pixel 3 XL. Feel free to update/commit new results and we can get the history using `git annotate`

```
benchmark:    13,239,690 ns Benchmark.moshi
benchmark:    14,983,907 ns Benchmark.apollo
benchmark:    25,381,565 ns Benchmark.apolloParseAndNormalize
benchmark:     4,657,449 ns Benchmark.apolloReadCacheMemory
benchmark:     7,913,594 ns Benchmark.apolloBatchCacheMemory
benchmark:   232,572,731 ns Benchmark.apolloReadCacheSql
benchmark:    69,434,069 ns Benchmark.apolloBatchCacheSql
```
