Change Log
==========

# Version 3.0.0

_2021-12-15_

This is the first stable release for ~Apollo Android 3~ Apollo Kotlin 3 🎉! 

There is [documentation](https://www.apollographql.com/docs/android/), a [migration guide](https://www.apollographql.com/docs/android/migration/3.0/) and a blog post coming soon (we'll update these notes when it's out). 

In a nutshell, Apollo Kotlin 3 brings:

* [coroutine APIs](https://www.apollographql.com/docs/android/essentials/queries/) for easier concurrency
* [multiplatform support](https://www.apollographql.com/docs/android/advanced/kotlin-native/) makes it possible to run the same code on Android, JS, iOS, MacOS and linux
* [responseBased codegen](https://www.apollographql.com/docs/android/advanced/response-based-codegen/) is a new optional codegen that models fragments as interfaces
* SQLite batching makes reading from the SQLite cache significantly faster
* [Test builders](https://www.apollographql.com/docs/android/advanced/test-builders/) offer a simple APIs to build fake models for your tests
* [The @typePolicy and @fieldPolicy](https://www.apollographql.com/docs/android/caching/declarative-ids/) directives make it easier to define your cache ids at compile time
* [The @nonnull](https://www.apollographql.com/docs/android/advanced/nonnull/) directive catches null values at parsing time, so you don't have to deal with them in your UI code

Feel free to ask questions by either [opening an issue on our GitHub repo](https://github.com/apollographql/apollo-android/issues), [joining the community](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client) or [stopping by our channel in the KotlinLang Slack](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)(get your invite [here](https://slack.kotl.in/)).

### Changes compared to `3.0.0-rc03`:

* Fix rewinding the Json stream when lists are involved (#3727)
* Kotlin 1.6.10 (#3723)
* Disable key fields check if unnecessary and optimize its perf (#3720)
* Added an easy way to log cache misses (#3724)
* Add a Data.toJson that uses reflection to lookup the adapter (#3719)
* Do not run the cache on the main thread (#3718)
* Promote JsonWriter extensions to public API (#3715)
* Make customScalarAdapters and subscriptionsNetworkTransport public (#3714)


# Version 3.0.0-rc03

_2021-12-13_

Compared to the previous RC, this version adds a few new convenience API and fixes 3 annoying issues.

💙 Many thanks to @ mateuszkwiecinski, @ schoeda and @ fn-jt for all the feedback 💙


## ✨ New APIs
- Make `ApolloCall.operation` public (#3698)
- Add `SubscriptionWsProtocolAdapter` (#3697)
- Add `Operation.composeJsonRequest` (#3697)

## 🪲 Bug fixes

- Allow repeated `@fieldPolicy` (#3686)
- Fix incorrect merging of nested objects in JSON (#3672)
- Fix duplicate query detection (#3699)


# Version 3.0.0-rc02

_2021-12-10_

💙 Many thanks to @michgauz, @joeldenke, @rohandhruva, @schoeda, @CoreFloDev and @sproctor for all the feedback 💙  

### ⚙️ [breaking] Merge ApolloQueryCall, ApolloSubscriptionCall, ApolloMutationCall (#3676)

In order to simplify the API and keep the symmetry with `ApolloRequest<D>` and `ApolloResponse<D>`, `ApolloQueryCall<D, E>`, `ApolloSubscriptionCall<D, E>`, `ApolloMutationCall<D, E>` are replaced with `ApolloCall<D>`. This change should be mostly transparent but it's technically a breaking change. If you are passing `ApolloQueryCall<D, E>` variables, it is safe to drop the second type parameter and use `ApolloCall<D>` instead.

### ✨ [New] Add `WebSocketNetworkTransport.reconnectWhen {}` (#3674)

You now have the option to reconnect a WebSocket automatically when an error happens and re-subscribe automatically after the reconnection has happened. To do so, use the `webSocketReconnectWhen` parameter: 

```kotlin
val apolloClient = ApolloClient.Builder()
    .httpServerUrl("http://localhost:8080/graphql")
    .webSocketServerUrl("http://localhost:8080/subscriptions")
    .wsProtocol(
        SubscriptionWsProtocol.Factory(
            connectionPayload = {
              mapOf("token" to upToDateToken)
            }
        )
    )
    .webSocketReconnectWhen {
      // this is called when an error happens on the WebSocket
      it is ApolloWebSocketClosedException && it.code == 1001
    }
    .build()
```

### Better Http Batching API (#3670)

The `HttpBatchingEngine` has been moved to an `HttpInterceptor`. You can now configure Http batching with a specific method:

```kotlin
apolloClient = ApolloClient.Builder()
    .serverUrl(mockServer.url())
    .httpBatching(batchIntervalMillis = 10)
    .build()
```

### All changes:

* Add 2.x symbols (`Rx2Apollo`, `prefetch()`, `customAttributes()`, `ApolloIdlingResource.create()`) to help the transition (#3679)
* Add canBeBatched var to ExecutionOptions (#3677)
* Merge ApolloQueryCall, ApolloSubscriptionCall, ApolloMutationCall (#3676)
* Add `WebSocketNetworkTransport.reconnectWhen {}` (#3674)
* Move BatchingHttpEngine to a HttpInterceptor (#3670)
* Add exposeErrorBody (#3661)
* fix the name of the downloadServiceApolloSchemaFromRegistry task (#3669)
* Fix DiskLruHttpCache concurrency (#3667)


# Version 3.0.0-rc01

_2021-12-07_

This version is the release candidate for Apollo Android 3 🚀. Please try it and [report any issues](https://github.com/apollographql/apollo-android/issues/new/choose), we'll fix them urgently.

There is [documentation](https://www.apollographql.com/docs/android/) and a [migration guide](https://www.apollographql.com/docs/android/migration/3.0/). More details are coming soon. In a nutshell, Apollo Android 3 brings, amongst other things:

* [coroutine APIs](https://www.apollographql.com/docs/android/essentials/queries/) for easier concurrency
* [multiplatform support](https://www.apollographql.com/docs/android/advanced/kotlin-native/) makes it possible to run the same code on Android, JS, iOS, MacOS and linux
* [responseBased codegen](https://www.apollographql.com/docs/android/advanced/response-based-codegen/) is a new optional codegen that models fragments as interfaces
* SQLite batching makes reading from the SQLite cache significantly faster
* [Test builders](https://www.apollographql.com/docs/android/advanced/test-builders/) offer a simple APIs to build fake models for your tests
* [The @typePolicy and @fieldPolicy](https://www.apollographql.com/docs/android/caching/declarative-ids/) directives make it easier to define your cache ids at compile time
* [The @nonnull](https://www.apollographql.com/docs/android/advanced/nonnull/) directive catches null values at parsing time, so you don't have to deal with them in your UI code


Compared to `beta05`, this version changes the default value of `generateOptionalOperationVariables`, is compatible with Gradle configuration cache and fixes a few other issues.

## ⚙️ API changes

### Optional operation variables (#3648) (breaking)

The default value for the `generateOptionalOperationVariables` config is now `true`.

What this means:

- By default, operations with nullable variables will be generated with `Optional` parameters
- You will need to wrap your parameters at the call site

For instance:

```graphql
query GetTodos($first: Int, $offset: Int) {
  todos(first: $first, offset: $offset) {
    ...Todo
  }
}
```

```kotlin
// Before
val query = GetTodosQuery(100, null)

// After
val query = GetTodosQuery(Optional.Present(100), Optional.Absent)

```

- If you prefer, you can set `generateOptionalOperationVariables` to `false` to generate non-optional parameters globally
- This can also be controlled on individual variables with the `@optional` directive
- More information about this can be found [here](https://www.apollographql.com/docs/android/advanced/operation-variables/)

We think this change will make more sense to the majority of users (and is consistent with Apollo Android v2's behavior) even though it may be more verbose, which is why it is possible to change the behavior via the `generateOptionalOperationVariables` config.

To keep the `beta05` behavior, set `generateOptionalOperationVariables` to false in your Gradle configuration:

```
apollo {
  generateOptionalOperationVariables.set(false)
}
```

### ApolloClient.Builder improvements (#3647)

You can now pass WebSocket related options to the `ApolloClient.Builder` directly (previously this would have been done via `NetworkTransport`):

```kotlin
// Before
val apolloClient = ApolloClient.Builder()
    // (...)
    .subscriptionNetworkTransport(WebSocketNetworkTransport(
        serverUrl = "https://example.com/graphql",
        idleTimeoutMillis = 1000L,
        wsProtocol = SubscriptionWsProtocol.Factory()
    ))
    .build()

// After
val apolloClient = ApolloClient.Builder()
    // (...)
    .wsProtocol(SubscriptionWsProtocol.Factory())
    .webSocketIdleTimeoutMillis(1000L)
    .build()
```

### Upgrade to OkHttp 4 (#3653) (breaking)

This version upgrades OkHttp to `4.9.3` (from `3.12.11`). This means Apollo Android now requires Android `apiLevel` `21`+. As OkHttp 3 enters end of life at the end of the year and the vast majority of devices now support `apiLevel` `21`, we felt this was a reasonable upgrade.

## 🪲 Bug fixes

- Fixed an issue where it was not possible to restart a websocket after a network error (#3646)
- Fixed an issue where Android Java projects could not use the Apollo Gradle plugin (#3652)


## 👷 All Changes

- Update a few dependencies (#3653)
- Fix Android Java projects (#3652)
- Expose more configuration options on ApolloClient.Builder (#3647)
- Fix restarting a websocket after a network error (#3646)
- Change optional default value (#3648)
- Fix configuration cache (#3645)

