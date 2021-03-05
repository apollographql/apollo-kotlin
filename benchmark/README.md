# Benchmark

This is a standalone Android Studio project to benchmark performance of the Json parser. While the root `apollo-android` project can be opened with IntelliJ, this one requires Android Studio.

It's not included in the composite build so it's easy to swap the Apollo version but it reuses the dependencies from the main project and declares OSS Snapshots and mavenLocal() as fallback repositories (in that order) so you can use `publishToMavenLocal` and snapshots easily.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will print the results in the `Run` window of Android Studio

## Current results:

Run on a Pixel 3 XL. Feel free to update/commit new results and we can get the history using `git annotate`

```
benchmark:    13,211,251 ns Benchmark.moshi
benchmark:    14,380,314 ns Benchmark.apollo
benchmark:    21,947,658 ns Benchmark.apolloParseAndNormalize
benchmark:     4,431,407 ns Benchmark.apolloReadCacheMemory
benchmark:     6,436,980 ns Benchmark.apolloBatchCacheMemory
benchmark:   222,846,324 ns Benchmark.apolloReadCacheSql
benchmark:    71,024,538 ns Benchmark.apolloBatchCacheSql
```

Same thing on a Samsung A5 2017 SM-A520F
```
benchmark:    27,272,962 ns Benchmark.moshi
benchmark:    28,992,191 ns Benchmark.apollo
benchmark:    43,999,000 ns Benchmark.apolloParseAndNormalize
benchmark:    12,343,846 ns Benchmark.apolloReadCacheMemory
benchmark:    16,767,616 ns Benchmark.apolloBatchCacheMemory
benchmark:   485,456,115 ns Benchmark.apolloReadCacheSql
benchmark:   125,179,230 ns Benchmark.apolloBatchCacheSql
```