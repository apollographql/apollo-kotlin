---
title: Cache migration guide
---

Up to Apollo Kotlin v4, the Normalized Cache used to be part of the Apollo Kotlin repository.

It is now hosted in [a dedicated repository](https://github.com/apollographql/apollo-kotlin-normalized-cache) and published at its own cadence and versioning scheme.

The previous codebase will not receive new features, it is deprecated in Apollo Kotlin v5, and it will be removed in the future. New developments happen in the new repository instead.

This guide highlights the main differences between the previous version and this new library, and how to migrate to it.

## Artifacts and packages

First, update the dependencies to your project:

```kotlin
// build.gradle.kts
dependencies {
  // Replace
  implementation("com.apollographql.apollo:apollo-normalized-cache") // Memory cache
  implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite") // SQLite cache
  
  // With
  implementation("com.apollographql.cache:normalized-cache:$cacheVersion") // Memory cache
  implementation("com.apollographql.cache:normalized-cache-sqlite:$cacheVersion") // SQLite cache
}
```

Note: the `com.apollographql.apollo:apollo-normalized-cache-api` artifact no longer exists, the code it contained has been merged into `com.apollographql.cache:normalized-cache`.

Then update your imports:

```kotlin
// Replace
import com.apollographql.apollo.cache.normalized.* 
// With
import com.apollographql.cache.normalized.*
```

```kotlin
// Replace
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
// With
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
```

## Linked directives

If you are using `@typePolicy` or `@fieldPolicy` directives, update your `extra.graphqls` file to link the new specification:

```graphql
# Replace
extend schema
@link(url: "https://specs.apollo.dev/kotlin_labs/v0.3", import: ["@typePolicy", "@fieldPolicy"])

# With
extend schema
@link(url: "https://specs.apollo.dev/cache/v0.4", import: ["@typePolicy", "@fieldPolicy"])
```

## Compiler plugin

Configure the [compiler plugin](./compiler-plugin/) in your `build.gradle.kts` file:

```kotlin
apollo {
  service("service") {
    // ...

    // Add this
    plugin("com.apollographql.cache:normalized-cache-apollo-compiler-plugin:$cacheVersion") {
    pluginArgument("com.apollographql.cache.packageName", packageName.get())
  }
}
```

In most cases, updating the coordinates/imports and adding the compiler plugin will be enough to migrate your project.
But there were also a few renames and API breaking changes - read on for more details.

## Database schema

The SQLite cache now uses a different schema.

- Previously, records were stored as JSON in a text column.
- Now they are stored in an equivalent binary format in a blob column.

When this library opens an existing database and finds the old schema, it will **automatically delete** any existing data and create the new schema.

> ⚠️ This is a destructive operation.

If your application relies on the data stored in the cache, you can manually transfer all the records from the an old database to a new one.
See an example on how to do that [here](https://github.com/apollographql/apollo-kotlin-normalized-cache/blob/main/tests/migration/src/commonTest/kotlin/MigrationTest.kt#L157).

Make sure you thoroughly test migration scenarios before deploying to production.

## `ApolloStore`

### Partial cache reads
`readOperation()` now returns an `ApolloResponse<D>` (it previously returned a `<D>`). This allows for returning [partial data](./partial-cache-reads/) from the cache, whereas
previously no data and a `CacheMissException` would be returned if any field was not found.

Now data with null fields (when possible) is returned with `Error`s in `ApolloResponse.errors` for any missing field

`ApolloResponse.cacheInfo.isCacheHit` will be false when any field is missing.

### Partial responses and errors are stored

Previously, partial responses were not stored by default, but you could opt in with `storePartialResponses(true)`.

Now `storePartialResponses()` is removed and is the default, and errors returned by the server are stored in the cache and `readOperation()` will return them.

By default, errors will not replace existing data in the cache. You can change this behavior with `errorsReplaceCachedValues(true)`.

> By default, partial responses are disabled so any missing or error fields are exposed as a response with no data (same behavior as previous versions). This
> is [configurable](./options/) with `cacheMissesAsException` and `serverErrorsAsException`.
>
> For more flexibility you can also implement your own fetch policy interceptor to handle partial cache reads, as shown in [this example](https://github.com/apollographql/apollo-kotlin-normalized-cache/blob/main/tests/partial-results/src/commonTest/kotlin/test/CachePartialResultTest.kt#L809).

### Publishing changes to watchers

Previously, write methods had 2 flavors:
- a `suspend` one that accepts a `publish` parameter to control whether changes should be published to watchers
- a non-suspend one (e.g. `writeOperationSync`) that doesn't publish changes

Now only the suspend ones exist and don't publish by default. Either pass `publish = true` or manually call `publish()` to notify watchers of changes.

```kotlin
// Replace
store.writeOperation(operation, data)
// With
store.writeOperation(operation, data, publish = true)
```

### Passing `CustomScalarAdapters` to `ApolloStore` methods

Previously, if you configured custom scalar adapters on your client, you had to pass them to the `ApolloStore` methods.

Now, `ApolloStore` has a reference to the client's `CustomScalarAdapters` so individual methods no longer need an adapters argument.

```kotlin
// Replace
client.apolloStore.writeOperation(
    operation = operation,
    data = data,
    customScalarAdapters = client.customScalarAdapters
)

// With
client.apolloStore.writeOperation(
    operation = operation,
    data = data
)
```

### Providing your own store

The `ApolloStore` interface has been renamed to `CacheManager`. If you provide your own implementation, change the parent interface to `CacheManager`.
Correspondingly, the `ApolloClient.Builder.store()` extension has been renamed to `ApolloClient.Builder.cacheManager()`.

```kotlin
// Replace
val MyStore = object : ApolloStore {
  // ...
}
val apolloClient = ApolloClient.Builder()
    // ...
    .store(MyStore)
    .build()

// With
val MyStore = object : CacheManager {
  // ...
}
val apolloClient = ApolloClient.Builder()
    // ...
    .cacheManager(MyStore)
    .build()
```

### Other changes

- All operations are now `suspend`.<br>
Note that they may **suspend** or **block** the thread depending on the underlying cache
implementation. For example, the SQL cache implementation on Android will **block** the thread while accessing the disk. As such,
these operations **must not** run on the main thread. You can enclose them in a `withContext` block with a `Dispatchers.IO` context to ensure
that they run on a background thread.

- `readFragment()` now returns a `ReadResult<D>` (it previously returned a `<D>`). This allows for surfacing metadata associated to the returned data, e.g. staleness.
- Records are now rooted per operation type (`QUERY_ROOT`, `MUTATION_ROOT`, `SUBSCRIPTION_ROOT`), when previously these were all at the same level, which could cause conflicts.

## `CacheResolver`, `CacheKeyResolver`

The APIs of `CacheResolver` and `CacheKeyResolver` have been tweaked to be more future-proof. The main change is that the methods now takes a `ResolverContext` instead of
individual parameters.

```kotlin
// Replace
interface CacheResolver {
  fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any?
}

// With
interface CacheResolver {
  fun resolveField(context: ResolverContext): Any?
}
```

`resolveField` can also now return a `ResolvedValue` when metadata should be returned with the resolved value (e.g. staleness).

If you wish to use the [Expiration](./expiration/) feature, a [`CacheControlCacheResolver`](https://apollographql.github.io/apollo-kotlin-normalized-cache/kdoc/normalized-cache/com.apollographql.cache.normalized.api/-cache-control-cache-resolver/index.html) should be used.
You can call the generated `cache()` extension on `ApolloClient.Builder` which will use it by default if max ages are configured in the schema.

### `TypePolicyCacheKeyGenerator`

You can now pass the type policies to the `TypePolicyCacheKeyGenerator` constructor, and it is recommended to do so.
The type policies are generated by the compiler plugin in `yourpackage.cache.Cache.typePolicies`.

If your entities ids are unique across the service, you can pass `CacheKey.Scope.SERVICE` to the `TypePolicyCacheKeyGenerator` constructor
to save space in the cache and improve hit rates in certain cases.

```kotlin
// Replace
val apolloClient = ApolloClient.Builder()
    // ...
    .normalizedCache(cacheFactory)
    .build()

// With
val apolloClient = ApolloClient.Builder()
    // ...
    .normalizedCache(
        cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator(
            typePolicies = Cache.typePolicies,
            keyScope = CacheKey.Scope.SERVICE // defaults to TYPE
        )
    )
    .build()
```

### `FieldPolicyCacheResolver`

- `FieldPolicyCacheResolver` now supports simple list cases. If your field takes a flat list of ids, you no longer need to implement a custom `CacheResolver` for it and can use `@fieldPolicy`.
- As for `TypePolicyCacheKeyGenerator`, you can pass `CacheKey.Scope.SERVICE` to the constructor if your ids are unique across the service:

```kotlin
val apolloClient = ApolloClient.Builder()
    // ...
    .normalizedCache(
        cacheFactory,
        cacheKeyGenerator = /*...*/,
        cacheResolver = FieldPolicyCacheResolver(
            keyScope = CacheKey.Scope.SERVICE // defaults to TYPE
        )
    )
    .build()
```

## CacheKey

For consistency, the `CacheKey` type is now used instead of `String` in more APIs, e.g.:

- `ApolloStore.remove()`
- `Record.key`
- `NormalizedCache.loadRecord()`

## Removed / deprecated APIs

- `ApolloCacheHeaders.EVICT_AFTER_READ` is removed. Manually call `ApolloStore.remove()` when needed instead.
- `NormalizedCache.remove(pattern: String)` is removed. Please open an issue if you need this feature back.

## Other changes

An `ApolloClient.Builder.cache()` extension function is generated by the compiler plugin, which configures the `CacheKeyGenerator`, `MetadataGenerator`, `CacheResolver`, and `RecordMerger` based
on the type policies, connection types, and max ages configured in the schema:

```kotlin
val apolloClient = ApolloClient.Builder()
    // ...
    .cache(cacheFactory = /*...*/)
    .build()
```
Optionally pass a `defaultMaxAge` (infinity by default) and `keyScope` (`TYPE` by default).
