# Benchmark

This is a standalone Android Studio project to benchmark performance. While the root `apollo-kotlin` project can be
opened with IntelliJ, this one requires Android Studio.

It's not a composite build so it's easy to swap the Apollo version and to workaround interop issues between AGP and multiplatform builds.
Use `publishAllPublicationsToPluginTestRepository` from `apollo-kotlin` to use the artifacts from the current version.

## Running the tests

You can run the tests from Android Studio by clicking the "run" icon in the gutter next to `class Benchmark`. This will
print the results in the `Run` window of Android Studio

## Inspiration

The `largesample` Json parsing data is taken from https://zacsweers.github.io/json-serialization-benchmarking/.
The `calendar_response` data was provided by @sebj and is a real-life use case.

## Current results:

You can get the current results at https://github.com/apollographql/apollo-kotlin/issues/4231


