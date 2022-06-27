# Benchmark

This is a standalone Android Studio project to benchmark performance. While the root `apollo-kotlin` project can be
opened with IntelliJ, this one requires Android Studio.

It's not a composite build so it's easy to swap the Apollo version and to workaround interop issues between AGP and multiplatform builds.
Use `publishAllPublicationsToPluginTestRepository` from `apollo-kotlin` to use the artifacts from the current version.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will
print the results in the `Run` window of Android Studio

## Current results:

Run on a Pixel 3 XL. Feel free to update/commit new results and we can get the history using `git annotate`

```
   15,155,158 ns Benchmark.apollo
   13,326,669 ns Benchmark.moshi
   19,175,002 ns Benchmark.apolloBatchCacheMemory
   44,868,183 ns Benchmark.apolloParseAndNormalize
  134,730,430 ns Benchmark.apolloBatchCacheSql
```

