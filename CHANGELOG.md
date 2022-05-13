Change Log
==========

# Version 3.3.1

_TBD_

This release starts tracking the MockServer public API. Even if the API remains experimental, we'll try to keep the number of breaking changes low in the future.



# Version 3.3.0

_2022-05-04_

This is the first release with [HMPP](https://kotlinlang.org/docs/multiplatform-hierarchy.html) support. If you're using multiplatform, updating to Kotlin 1.6.21 is strongly encouraged. 

This release also brings WebSocket related improvements and other fixes!

## ✨️ [new] Hierarchical MultiPlatform Project (HMPP) (#4033)

When using Apollo Kotlin on a multiplatform project, this release is compatible with the [hierarchical project structure](https://kotlinlang.org/docs/multiplatform-hierarchy.html), which makes it easier to share common code among several targets. Using HMPP in your project also fixes some issues when compiling Kotlin metadata. See https://github.com/apollographql/apollo-kotlin/issues/4019 and https://youtrack.jetbrains.com/issue/KT-51970/ for more details.

**✋ Note**: If you're using multiplatform, we strongly encourage updating to Kotlin 1.6.21. If that is not an option, you might have issues resolving dependencies. More infos in [this issue](https://github.com/apollographql/apollo-kotlin/issues/4095#issuecomment-1123571706).

## ✨️ [new] `WebSocketNetworkTransport.closeConnection` (#4049)

This new method can be used in conjunction with [`reopenWhen`](https://apollographql.github.io/apollo-kotlin/kdoc/apollo-runtime/com.apollographql.apollo3/-apollo-client/-builder/web-socket-reopen-when.html)  to force a reconnection to the server. This could be useful for instance when needing to pass new auth tokens in the headers. If you were using `subscriptionManager.reconnect()` in 2.x, `closeConnection` is a simple way to achieve the same behaviour.


## ✨️ [new] `GraphQLWsProtocol.connectionPayload` is now a lambda (#4043)

With `GraphQLWsProtocol`, if you need to pass parameters to the connection payload, previously you would pass them as a static map to the builder. With this change you can now pass a lambda providing them as needed. This facilitates passing fresh auth tokens when connecting.

## ✨️ [new] Add insecure option to download schema (#4021)

You can now use the `--insecure` flag when downloading a schema with [`downloadApolloSchema`](https://www.apollographql.com/docs/kotlin/advanced/plugin-configuration/#downloading-a-schema), to bypass the certificate check, which can be useful if a server is configured with a self-signed certificate for instance.

## 👷‍ All changes

- Add WebSocketNetworkTransport.closeConnection (#4049)
- Made connectionPayload as suspend function in GraphQLWsProtocol (#4043)
- ⚡ Ignore unknown websocket messages (#4066)
- Kotlin 1.6.21 & HMPP (#4033)
- Provide a Content-Length when using Upload (#4056)
- ☁️ add HttpRequest.newBuilder(url, method) (#4038)
- Escape enum constants (#4035)
- Fix the Moshi adapter used for OperationOutput. Moshi cannot get the type parameters from the typealias
  automagically (#4022)
- Add insecure option to download schema (#4021)
- Try to reduce allocations in MapJsonReader (#3935)
- 🔒 Deprecate BearerTokenInterceptor and provide tests and docs instead (#4068)

## ❤️ External contributors

Many thanks to @CureleaAndrei and @kdk96 for contributing to this release! 🙏

## ⚙️ Deprecations

- `BearerTokenInterceptor` was provided as an example but is too simple for most use cases, and has therefore been deprecated
  in this release. [This page](https://www.apollographql.com/docs/kotlin/advanced/authentication) provides more details
  about authentication.
- The previous ways of passing parameters to the connection payload with `GraphQLWsProtocol` has been deprecated (see above).

# Version 3.2.2

_2022-04-11_

A maintenance release to fix the `addJvmOverloads` option added in 3.2.0 as well as other fixes. If you're using APQs, the mutations are now always send using `POST`. See [#4006](https://github.com/apollographql/apollo-kotlin/issues/4006#issuecomment-1092628783) for details and a way to override the behaviour if you really need to.

Many thanks to @benedict-lim, @olivierg13, @konomae and @sproctor for their contributions 💙 

## 👷‍ All changes

* Use a constant for JvmOverloads to avoid a crash due to relocation (#4008)
* Always use POST for Mutations in APQs (Auto Persisted Queries) (#4011)
* Add configurable headers to WebSocketNetworkTransport (#3995)
* Handle SqlNormalizedCache merge APIs Exceptions with ApolloExceptionHandler (#4002)
* Add adapter for java.time.OffsetDateTime (#4007)
* ⏰  Add tests for date adapters (#3999)
* Fix wrong LocalDate and LocalDateTime formats in JavaTimeAdapters (#3997)

# Version 3.2.1

_2022-04-05_

This release introduces a few improvements and bug fixes.

## ✨️ [new] `ApolloCall<D>.emitCacheMisses(Boolean)` (#3980)

When observing the cache with `watch`, the behavior was to not emit cache misses at all, which may not desirable in certain cases. With this new option, you can now choose to emit them: in that case responses will be emitted with a null `data`.

This can be used like so:

```kotlin
apolloClient.query(query)
  .fetchPolicy(FetchPolicy.CacheOnly)
  .emitCacheMisses(true)
  .watch()
  .collect { response ->
    // response.data will be null in case of cache misses
  }
```

This is also closer to the behavior that was in place in v2. Many thanks to @mateuszkwiecinski for the insights and raising the issue!

## ⚙️ [breaking] Allow configuration of frame types used in `SubscriptionWsProtocol` and default to Text (#3992)

When using subscriptions over WebSockets with `SubscriptionWsProtocol` (the default), the frames were sent in the binary format. It was reported that this was not compatible with certain servers ([DGS](https://netflix.github.io/dgs), [graphql-java-kickstart](https://github.com/graphql-java-kickstart/graphql-spring-boot)) that are expecting text frames. This is now fixed and the default is to send text frames.

> ⚠️ This may be a breaking change if your server expects binary frames only! 

If that is the case, you can use the new `frameType` option to configure the frame type to be sent:

```kotlin
client = ApolloClient.Builder()
  .webSocketServerUrl("wss://...")
  .wsProtocol(GraphQLWsProtocol.Factory(frameType = WsFrameType.Binary))
  .build()
```

Many thanks to @Krillsson and @aviewfromspace1 for the insights and raising the issue!

## 👷‍ All changes

* Allow configuration of frame types used in SubscriptionWsProtocol and default to Text (#3992)
* add `ApolloRequest.newBuilder(operation: Operation<E>)`  (#3988)
* Add exception handlers to ApolloCacheInterceptor and SqlNormalizedCache (#3989)
* 📠 Fix some @DeprecatedSince annotations (#3983)
* 👓  add ApolloCall<D>.emitCacheMisses(Boolean) (#3980)
* ⚙️  Fix fragments on the root query type in operationBased codegen (#3973)

## ❤️ External contributors

Many thanks to @AdamMTGreenberg and @Krillsson for the contributions! 🙏


# Version 3.2.0

_2022-03-29_

💙 Thanks to @undermark5, @demoritas, @rkoron007, @akshay253101, @StylianosGakis, @Goooler, @jeffreydecker, @theBradfo, @anderssandven and @olivierg13 for contributing to this release.

This version adds JS WebSocket support, more options to deal with `__typename` amongst other features and bugfixes.

## ✨️ [new] JS WebSocket support (#3913)

Version 3.2.0 now has WebSocket support for Javascript targets courtesy of @undermark5! This is a huge milestone and means the JS target is now even closer to its JVM and iOS counterparts.

|  | `jvm` | Apple | `js` | `linuxX64`
| --- | :---: |:-----:|:----:| :---: |
| `apollo-api` (models)|✅|   ✅   |  ✅   |✅|
| `apollo-runtime` (network, query batching, apq, ...) |✅|   ✅   |  ✅   |🚫|
| `apollo-normalized-cache` |✅|   ✅   |  ✅   |🚫|
| `apollo-adapters` |✅|   ✅   |  ✅   |🚫|
| `apollo-normalized-cache-sqlite` |✅|   ✅   |  🚫  |🚫|
| `apollo-http-cache` |✅|  🚫   |  🚫  |🚫|

The implementation is based on the [`ws`](https://github.com/websockets/ws) library on Node and the [`WebSocket` API](https://websockets.spec.whatwg.org//) on the browser and inspired by [Ktor](https://ktor.io/).

## ✨️ [new] Fine grained `__typename` control (#3939)

This version generates non-nullable fragments when it knows the fragment is always present:

```graphql
{
  cat {
    # Because Animal is a supertype of Cat this condition will always be true
    ... on Animal {
      species
    }
  }
}
```

In addition, it introduces a `addTypename` Gradle option to have better control over when to add the `__typename` field:

```kotlin
/**
 * When to add __typename. One of "always", "ifFragments", "ifAbstract" or "ifPolymorphic"
 *
 * - "always": Add '__typename' for every compound field
 *
 * - "ifFragments": Add '__typename' for every selection set that contains fragments (inline or named)
 * This is adding a lot more '__typename' than the other solutions and will be certainly removed in
 * a future version. If you require '__typename' explicitly, you can add it to your queries.
 * This causes cache misses when introducing fragments where no fragment was present before and will be certainly removed in
 * a future version.
 *
 * - "ifAbstract": Add '__typename' for abstract fields, i.e. fields that are of union or interface type
 * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
 * could be read from the cache and we don't have a containing field in that case.
 *
 * - "ifPolymorphic": Add '__typename' for polymorphic fields, i.e. fields that contains a subfragment
 * (inline or named) whose type condition isn't a super type of the field type.
 * If a field is monomorphic, no '__typename' will be added.
 * This adds the bare minimum amount of __typename but the logic is substantially more complex and
 * it could cause cache misses when using fragments on monomorphic fields because __typename can be
 * required in some cases.
 *
 * Note: It also adds '__typename' on fragment definitions that satisfy the same property because fragments
 * could be read from the cache and we don't have a containing field in that case.
 *
 * Default value: "ifFragments"
 */
```

You can read more in the corresponding [Typename.md](https://github.com/apollographql/apollo-kotlin/blob/main/design-docs/Typename.md) design document.


## ✨️ [new] Maven publishing for multi-module apollo metadata (#3904)

The Apollo Gradle plugin now creates a new "apollo" publication if `maven-publish` is found. This means you can now publish the Apollo metadata to a maven repository:

```bash
# In your producer project
./gradlew publishApolloPublicationTo[SomeRepository]
``` 

Assuming your producer project is using `com.example:project:version` for maven coordinates, the Apollo metadata will be published at `com.example:project-apollo:version`:

```kotlin
// In your consumer project
dependencies {
  implementation("com.example:project:version")
  apolloMetadata("com.example:project-apollo:version")
}
```

**Note**: There are absolutely no forward/backward compatibility guarantees for Apollo metadata yet. The Apollo version used in the consumer **must** be the same as the one used in the producer.

## ✨️ [new] `addJvmOverloads` Gradle option (#3907)

For better Java interop, you can now opt-in `addJvmOverloads`. `addJvmOverloads` will add the `@JvmOverloads` to your Kotlin operations:

```kotlin
@JvmOverloads
class GetHeroQuery(val id: String, val episode: Optional<Episode> = Optional.Absent) {
  // ...
}
```

Meaning you can now create a new query from Java without having to specify `episode`: `new GetHeroQuery("1002")`

## 👷‍ All changes 

* 📖 Add note to tutorial about `graphql-ws` library to tutorial (#3961)
* Use ApolloCompositeException for HTTP CachePolicies (#3967)
* 🖋️ bump kotlin poet to 1.11.0 (#3970)
* Add underlying exceptions as suppressed exceptions in ApolloCompositeException (#3957)
* Add macosArm64 and macosX64 targets (#3954)
* JS Websockets: handle remote close (#3952)
* ⚙️ Introduce addTypename Gradle parameter (#3939)
* Optimize CI a bit (#3942)
* Add more field merging diagnostics (#3937)
* ⚙️ Make adapters code work without relying on having a` __typename` IrProperty (#3930)
* Add equals and hashCode implementations for models with no properties (#3928)
* 🐘 Unbreak Gradle configuration cache (#3918)
* WebSocket support for JS targets (#3913)
* 🗄️ add apolloClient.httpCache (#3919)
* ⚙️ Detect case insensitive filesystems (like MacOS default one) and rename classes when that happens (#3911)
* Fix exceptions where not caught when reading the body of a batched query (#3910)
* 🗄️ Fix writing fragments programmatically was using the wrong cache key (#3905)
* 📦 Maven publishing for Apollo metadata (#3904)
* Add addJvmOverloads Gradle option for better Java interop (#3907)
* 🐘 Fix using refreshVersions (#3898)
* add support for triple quotes escapes (#3895)
* 👷 Test Builders: Fix enums in test resolver (#3894)
* Validation: Detect missing arguments when there are no arguments at all (#3893)
* Add support for receiving multiple bodies with multipart (#3889)
* ✅  Validation: allow nullable variables in non-null locations if there is a default value (#3879)
* 🗄️  HttpCache: do not cache mutations by default (#3873)
* Chunked Transfer-Encoding support in MockServer (#3870)
* Fix -1 body length in BatchingHttpInterceptor (#3874)
* Fix issue in Java codegen where selectors returned ImmutableMapBuilder instances instead of Map (#3861)
* Make watchers subscribe to the store earlier (#3853)

# Version 3.1.0

_2022-02-07_

Version 3.1.0 introduces new APIs for testing, mapping scalars as well a redesigned cache pipeline.
It also contains bugfixes around the `@include` directives, MemoryCache and GraphQL validation amongst other changes.

## ⚙️ [breaking] Fragment package name and `useSchemaPackageNameForFragments` (#3775)

If you're using `packageNamesFromFilePaths()`, the package name of generated fragment classes has changed. 

Different generated types have different package names:

* Generated types coming from operations are generated based on the operation path
* Generated types coming from the schema (input objects, custom scalars and enums) are generated based on the schema path

Previously, fragments were using the schema path which is inconsistent because fragments are not defined in the schema but are executable files, like operations. 

Version 3.1.0 now uses the same logic for fragments as for operations. To revert to the previous behaviour, you can use `useSchemaPackageNameForFragments`:

```kotlin
apollo {
  useSchemaPackageNameForFragments.set(true)
}
```

This is also done automatically if you're using `useVersion2Compat()`. Moving forward, the plan is to remove `useSchemaPackageNameForFragments` in favor of setting a custom `PackageNameGenerator`. If you have use cases that require `useSchemaPackageNameForFragments`, please [reach out](https://github.com/apollographql/apollo-kotlin/issues/new?assignees=&labels=%3Aquestion%3A+Type%3A+Question&template=question.md&title=).

## ✨ [New] `QueueTestNetworkTransport` (#3757)

3.1.0 introduces `QueueTestNetworkTransport` to test at the GraphQL layer without needing to run an HTTP server.

To use it, configure your `ApolloClient`:

```kotlin
// This uses a QueueTestNetworkTransport that will play the queued responses
val apolloClient = ApolloClient.Builder()
    .networkTransport(QueueTestNetworkTransport())
    .build()
```

You can then use the `enqueueTestResponse` extension function to specify the GraphQL responses to return:

```kotlin
val testQuery = GetHeroQuery("001")
val testData = GetHeroQuery.Data {
  hero = droidHero {
    name = "R2D2"
  }
}
apolloClient.enqueueTestResponse(testQuery, testData)
val actual = apolloClient.query(testQuery).execute().data!!
assertEquals(testData.hero.name, actual.hero.name)
```

## ✨ [New] `MockServerHandler` (#3757)

If you're testing at the HTTP layer, you can now define your own `MockServerHandler` to customize how the server is going to answer to requests:

```kotlin
val customHandler = object : MockServerHandler {
  override fun handle(request: MockRequest): MockResponse {
    return if (/* Your custom logic here */) {
      MockResponse(
          body = """{"data": {"random": 42}}""",
          headers = mapOf("X-Test" to "true"),
      )
    } else {
      MockResponse(
          body = "Internal server error",
          statusCode = 500,
      )
    }
  }
}
val mockServer = MockServer(customHandler)
```

## ✨ [New] `FetchPolicy.CacheAndNetwork` (#3828)

Previously, `FetchPolicy`s were limited to policies that emitted at most **one** response. There was a `executeCacheAndNetwork()` method but it felt asymmetrical. This version introduces `FetchPolicy.CacheAndNetwork` that can emit up to two responses:

```kotlin
apolloClient.query(query)
  // Check the cache and also use the network (1 or 2 values can be emitted)
  .fetchPolicy(FetchPolicy.CacheAndNetwork)
  // Execute the query and collect the responses
  .toFlow().collect { response ->
      // ...
  }
```

## ✨ [New] `ApolloCall<D>.fetchPolicyInterceptor(interceptor: ApolloInterceptor)` (#3743)

If you need more customized ways to fetch data from the cache or more fine-grained error handling that does not come with the built-in `FetchPolicy`, you can now use `fetchPolicyInterceptor`:

```kotlin
// An, interceptor that will only use the network after getting a successful response
val refetchPolicyInterceptor = object : ApolloInterceptor {
  var hasSeenValidResponse: Boolean = false
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return if (!hasSeenValidResponse) {
      CacheOnlyInterceptor.intercept(request, chain).onEach {
        if (it.data != null) {
          // We have valid data, we can now use the network
          hasSeenValidResponse = true
        }
      }
    } else {
      // If for some reason we have a cache miss, get fresh data from the network
      CacheFirstInterceptor.intercept(request, chain)
    }
  }
}

apolloClient.query(myQuery)
    .refetchPolicyInterceptor(cacheOnlyInterceptor)
    .watch()
    .collect {
      //
    }
```

## ✨ [New] `Service.mapScalar` Gradle API (#3779)

You can now use `mapScalar` to specify your scalar mappings:

```kotlin
apollo {
  // Replace 
  customScalarsMapping.set(mapOf(
      "Date" to "java.util.Date"
  ))
  
  // With
  mapScalar("Date", "java.util.Date")
}
```

`mapScalar` also works with built-in scalar types so you can map the `ID` type to a kotlin Long:

```kotlin
apollo {
  // This requires registering an adapter at runtime with `addCustomScalarAdapter()` 
  mapScalar("ID", "kotlin.Long")
}
```

As an optimization, you can also provide the adapter at compile time. This will avoid a lookup at runtime everytime such a scalar is read:

```kotlin
apollo {
  // No need to call `addCustomScalarAdapter()`, the generated code will use the provided adapter 
  mapScalar("ID", "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")
}
```

For convenience, a helper function is provided for common types:

```kotlin
apollo {
  // The generated code will use `kotlin.Long` and the builtin LongAdapter 
  mapScalarToKotlinLong("ID")

  // The generated code will use `kotlin.String` and the builtin StringAdapter
  mapScalarToKotlinString("Date")

  // The generated code will use `com.apollographql.apollo3.api.Upload` and the builtin UploadAdapter
  mapScalarToUpload("Upload")
}
```

## 🚧 [Changed] `convertApolloSchema` and `downloadApolloSchema` now use paths relative to the root of the project (#3773, #3752)

Apollo Kotlin adds two tasks to help to manage schemas: `convertApolloSchema` and `downloadApolloSchema`. These tasks are meant to be used from the commandline.

Previously, paths were interpreted using the current working directory with `File(path)`. Unfortunately, this is unreliable because Gradle might change the current working directory in some conditions (see [Gradle#13927](https://github.com/gradle/gradle/issues/13927) or [Gradle#6074](https://github.com/gradle/gradle/issues/6074) for an example).

With 3.1.0 and onwards, paths, will be interpreted relative to the root project directory (`project.rootProject.file(path)`):

```
# schema is now interpreted relative to the root project directory and
# not the current working directory anymore. This example assumes there 
# is a 'app' module that applies the apollo plugin
./gradlew downloadApolloSchema \
  --endpoint="https://your.domain/graphql/endpoint" \
  --schema="app/src/main/graphql/com/example/schema.graphqls"
```

## ❤️ External contributors

Many thanks to @dhritzkiv, @mune0903, @StylianosGakis, @AchrafAmil and @jamesonwilliams for their awesome contributions! You rock 🎸 🤘 !

## 👷 All changes

* Fix error reporting when there is a "schema.graphqls" but it doesn't contain any type definition (#3844)
* Make guessNumber read the next value only once, fixes parsing custom scalars without a custom adapter (#3839, #3836)
* Clarify need to pass client's customScalarAdapters to store methods (#3838)
* Fix null pointer exception in LruCache while trimming (#3833)
* Add FetchPolicy.CacheAndNetwork (#3828)
* Allow to specify error handling for watch() (#3817)
* Scalar mapping and adapter configuration improvements (#3779)
* Tunnel variables in CustomScalarAdapters (#3813)
* Terminate threads correctly if no subscription has been executed (#3803)
* fix validation of merged fields (#3799)
* Make `reconnectWhen` suspend and pass attempt number (#3772)
* Merge HTTP headers when batching (#3776)
* MockServer improvements and TestNetworkTransport (#3757)
* Fix calling ApolloClient.newBuilder() if the original ApolloClient used `.okHttpClient` (#3771)
* Make `convertApolloSchema` and `downloadApolloSchema` use path from the root of the project (#3773, #3752)
* fix fragment package name in multi-module scenarios (#3775)
* Make the error printer robust to unknown source locations, fixes schemas with duplicate types (#3753)
* Allow to customize the fetchPolicy with interceptors (#3743)

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

