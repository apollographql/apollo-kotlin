# Builders

This outlines the necessary changes to go to a Builders oriented APIs for `ApolloClient` and `ApolloRequest` in 3.x, following [this RFC](https://github.com/apollographql/apollo-android/issues/3301).

## Context

- On the `main` (2.x) branch, the `ApolloClient` API already uses Builders (`ApolloRequest` doesn't exist)
- On the 3.x branch, both APIs use With-ers
    - These use `ExecutionContext` as a generic way to augment the client / request
- [This RFC](https://github.com/apollographql/apollo-android/issues/3301) outlines some pros/cons of both approaches, as well as an alternative DSL one, and asks the community for feedback.
    - There were not a lot of replies but Builders appear to be favored.

## `ApolloClient`

### Current situation

Currently has 2 constructors:

**Short:**

```kotlin
constructor(
    serverUrl: String,
)
```

Usage:

```kotlin
val apolloClient = ApolloClient("http://example.com")
```

**Full:**

```kotlin
constructor(
    networkTransport: NetworkTransport,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    subscriptionNetworkTransport: NetworkTransport = networkTransport,
    interceptors: List<ApolloInterceptor> = emptyList(),
    executionContext: ExecutionContext = ExecutionContext.Empty,
    requestedDispatcher: CoroutineDispatcher? = null,
)
```

Usage:

```kotlin
val apolloClient = ApolloClient(
        // Mandatory
        networkTransport = ...,

        // Optional
        customScalarAdapters = ...,
        subscriptionNetworkTransport = ...,
        interceptors = ...,
        executionContext = ...,
        requestedDispatcher = ... ,
    )
```

Current list of existing `with` methods:

### Member methods

- `withCustomScalarAdapter`
- `withInterceptor`
- `withExecutionContext`

Minor detail: `customScalarAdapters`, `interceptors` and `executionContext` can both be passed to the constructor and using the With-ers. I propose we don't keep this duality with the Builder: having one way to do things is easier to grasp.

### Extensions

| Extension                | Module                  |
|--------------------------|-------------------------|
| withAutoPersistedQueries | apollo-runtime          |
| withHttpCache            | apollo-http-cache       |
| withStore                | apollo-normalized-cache |
| withNormalizedCache      | apollo-normalized-cache |
| withFetchPolicy          | apollo-normalized-cache |
| withRefetchPolicy        | apollo-normalized-cache |
| withIdlingResource       | apollo-idling-resource  |

### Proposed API

```kotlin
val apolloClient = ApolloClient.Builder()
        // Built-in
        .serverUrl(...)
        .networkTransport(...)
        .subscriptionNetworkTransport(...)
        .requestedDispatcher(...)
        .addCustomScalarAdapter(...)
        .addInterceptor(...)
        .addExecutionContext(...)
        
        // Extensions
        .autoPersistedQueries(...)
        .httpCache(...)
        // etc.

        .build()
```

Note: at least `serverUrl()` or `networkTransport()` must be called, otherwise an exception will be raised when calling `build()`.

Naming:

- single object: `fieldName(fieldName: ObjectType)`
- collections: `addFieldName(fieldName: ObjectType`

### `Builder` inner class

Here's a preview of what the `Builder` inner class should look like.

⚠️ Note: not all the code is shown, for brevity.

```kotlin
class Builder private constructor(
    var networkTransport: NetworkTransport?,
    private var subscriptionNetworkTransport: NetworkTransport?,
    private var customScalarAdapters: MutableMap<String, Adapter<*>>,
    private val interceptors: MutableList<ApolloInterceptor>,
    private var requestedDispatcher: CoroutineDispatcher?,
    override var executionContext: ExecutionContext,
) : ExecutionParameters<Builder> {

  constructor() : this(
    networkTransport = null,
    subscriptionNetworkTransport = null,
    customScalarAdapters = mutableMapOf(),
    interceptors = mutableListOf(),
    requestedDispatcher = null,
    executionContext = ExecutionContext.Empty,
  )

  fun ApolloClient.newBuilder(): Builder {
    return Builder(...)
  }

  fun serverUrl(serverUrl: String): Builder {
    networkTransport = HttpNetworkTransport(serverUrl = serverUrl)
    subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl)
    return this
  }

  fun networkTransport(networkTransport: NetworkTransport): Builder {
    this.networkTransport = networkTransport
    return this
  }

  fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport): Builder { ... }

  fun <T> addCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>): Builder {
    customScalarAdapters[customScalarType.name] = customScalarAdapter
    return this
  }

  fun addInterceptor(interceptor: ApolloInterceptor): Builder {
    interceptors += interceptor
    return this
  }
  
  fun requestedDispatcher(requestedDispatcher: CoroutineDispatcher): Builder { ... }

  override fun withExecutionContext(executionContext: ExecutionContext): Builder {
    this.executionContext = this.executionContext + executionContext
    return this
  }

  fun build(): ApolloClient {
    check(networkTransport != null) {
      "NetworkTransport not set, please call either serverUrl() or networkTransport()"
    }
    return ApolloClient(
            networkTransport = networkTransport!!,
            subscriptionNetworkTransport = subscriptionNetworkTransport ?: networkTransport!!,
            customScalarAdapters = CustomScalarAdapters(customScalarAdapters),
            interceptors = interceptors,
            ...
    )
  }
}
```

Note: the private constructor exists solely to be called by the `newBuilder()` method.

### Updates to `ApolloClient`

- Delete short hand constructor
- Make main constructor `private`, and remove parameters' default values (`@JvmOverloads` can also be removed)
- Delete with-ers:
    - `withCustomScalarAdapter`
    - `withInterceptor`
    - `withExecutionContext`
- Make the `copy` fun `private`
    - Note: `withExecutionContext` is using `copy` which is why we can't just delete it. We could instead make `executionContext` a var with a `private set`, and get rid of `copy`?
- Make `networkTransport`  `private`

### About `ExecutionParameters`

Currently this interface is implemented by `ApolloClient` and `ApolloRequest`:

```kotlin
interface ExecutionParameters<T> where T : ExecutionParameters<T> {
  val executionContext: ExecutionContext
  fun withExecutionContext(executionContext: ExecutionContext): T
}
```

We will instead split this interface in 2:
- `HasExecutionContext`: implemented by `ApolloClient` and `ApolloRequest`
- `HasMutableExecutionContext`: implemented by the Builders.

There are places in the code where extensions are called directly on `ApolloClient` or `ApolloRequest`.
In order to keep this ability, we'll add a `newBuilder()` method, allowing to "mutate" these 2 classes.

For instance:

**Before**
```kotlin
queryRequest.withFetchPolicy(FetchPolicy.CacheOnly)
```

**After**
```kotlin
queryRequest.newBuilder().fetchPolicy(FetchPolicy.CacheOnly).build()
```

Another example is an interceptor wanting to add custom headers. This would look like:

```kotlin
class MyInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    chain.proceed(
      request
        .newBuilder()
        .addHttpHeader("myHeader", "value")
        .build()
    )
  }
}
```

### Migrate extensions to use Builder

With-ers extensions on `ApolloClient`, should now be on `ApolloClient.Builder` instead.

Here's an example:

**Before**

```kotlin
fun ApolloClient.withAutoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient {
  return withInterceptor(AutoPersistedQueryInterceptor(httpMethodForDocumentQueries)).let {
    if (hashByDefault) {
      it.withHttpMethod(httpMethodForHashedQueries).withHashedQuery(true)
    } else {
      it
    }
  }
}
```

**After**

```kotlin
fun ApolloClient.Builder.autoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient.Builder {
  return addInterceptor(AutoPersistedQueryInterceptor(httpMethodForDocumentQueries)).let {
    if (hashByDefault) {
      it.withHttpMethod(httpMethodForHashedQueries).withHashedQuery(true)
    } else {
      it
    }
  }
}
```

## `ApolloRequest`

### Current situation

One constructor:

```kotlin
class ApolloRequest<D : Operation.Data>(
    val operation: Operation<D>,
    val requestUuid: Uuid = uuid4(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty,
)
```

**Usage**

```kotlin
val request = ApolloRequest(operation)
    .withHttpExpireTimeout(500)
    .withHttpFetchPolicy(HttpFetchPolicy.NetworkOnly)
    .with(...)
```

There are no With-ers directly on `ApolloRequest` but, as `ApolloClient`, it implements `ExecutionParameters` so a lot of extensions can be applied to a request, via `withExecutionContext`.

### Proposed API

```kotlin
val request = ApolloRequest.Builder(operation)
    .httpExpireTimeout(500)
    .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)
    ...
    .build()
```

### `Builder` inner class

The `Builder` should look like this:

```kotlin
class Builder<D : Operation.Data>(
    var operation: Operation<D>,
    var requestUuid: Uuid = uuid4(),
) : ExecutionParameters<Builder<D>> {

  override var executionContext: ExecutionContext = ExecutionContext.Empty
  
  override fun withExecutionContext(executionContext: ExecutionContext): Builder<D> {
    this.executionContext = this.executionContext + executionContext
    return this
  }
  
  fun build(): ApolloRequest<D> {
    return ApolloRequest(
        operation = operation,
        requestUuid = requestUuid,
        executionContext = executionContext,
    )
  }
}
```

## Rename other With-ers

For consistency, all With-ers (extensions on `ExecutionParameters`) should be renamed, e.g.:

- `withWriteToCacheAsynchronously` → `writeToCacheAsynchronously`
- `withHttpHeader` → `httpHeader`
- etc.

## Update usage

- Tests
- Documentation
- Migration guide
