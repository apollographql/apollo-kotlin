# Benchmark

This is a standalone Android Studio project to benchmark performance. 

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter. This will
print the results in the `Run` window of Android Studio.

For the command line, use:

```
# macrobenchmarks
./gradlew -p benchmark :macrobenchmark:connectedBenchmarkAndroidTest 

# microbenchmarks
./gradlew -p benchmark :microbenchmark:benchmarkReport
```

## Inspiration

The `largesample` Json parsing data is taken from https://zacsweers.github.io/json-serialization-benchmarking/.
The `calendar_response` data was provided by @sebj and is a real-life use case.

## Current results:

You can get the current results at https://github.com/apollographql/apollo-kotlin/issues/4231


