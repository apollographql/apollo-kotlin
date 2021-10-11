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
    flowDecorators: List<FlowDecorator> = emptyList(),
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
        flowDecorators = ...
    )
```

Current list of existing `with` methods:

### Member methods

- `withCustomScalarAdapter`
- `withInterceptor`
- `withFlowDecorator`
- `withExecutionContext`

Minor detail: `customScalarAdapters`, `interceptors`, `executionContext` and `flowDecorators` can both be passed to the constructor and using the With-ers. I propose we don't keep this duality with the Builder: having one way to do things is easier to grasp.

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

**Short**

```kotlin
val apolloClient = ApolloClient.Builder("http://example.com").build()
```

**Full**

```kotlin
val apolloClient = ApolloClient.Builder(
          // Mandatory
          networkTransport = ...,
          subscriptionNetworkTransport = ...,
          executionContext = ...,
       )
          // Optional (members)
          .requestedDispatcher(...)
          .addCustomScalarAdapter(...)
          .addInterceptor(...)
          .addFlowDecorator(...)
          .addExecutionContext(...)

          // Optional (extensions)
          .autoPersistedQueries(...)
          .httpCache(...)
          // etc.

          .build()
```

❓ Is the `executionContext` constructor parameter really needed? If not we could delete it to simplify the API.

Naming:

- single object: `fieldName(fieldName: ObjectType)`
- collections: `addFieldName(fieldName: ObjectType`

### `Builder` inner class

Here's a preview of what the `Builder` inner class should look like.

⚠️ Note: not all fields are shown, for brievety. 

```kotlin
class Builder private constructor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    override val executionContext: ExecutionContext,
    private var requestedDispatcher: CoroutineDispatcher?,
    private val interceptors: List<ApolloInterceptor>,
) : ExecutionParameters<Builder> {

  constructor(
      networkTransport: NetworkTransport,
      subscriptionNetworkTransport: NetworkTransport = networkTransport,
      executionContext: ExecutionContext = ExecutionContext.Empty, // <- not sure if this parameter is useful?
  ) : this(
      networkTransport = networkTransport,
      subscriptionNetworkTransport = subscriptionNetworkTransport,
      executionContext = executionContext,
      requestedDispatcher = null,
      interceptors = mutableListOf()
  )

  /**
   * A short-hand constructor
   */
  constructor(
      serverUrl: String,
  ) : this(
      networkTransport = HttpNetworkTransport(serverUrl = serverUrl),
      subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl),
  )

  fun requestedDispatcher(requestedDispatcher: CoroutineDispatcher): Builder {
    this.requestedDispatcher = requestedDispatcher
    return this
  }

  fun addInterceptor(interceptor: ApolloInterceptor): Builder {
    return copy(
        interceptors = interceptors + interceptor
    )
  }

  override fun withExecutionContext(executionContext: ExecutionContext): Builder {
    return copy(
        executionContext = this.executionContext + executionContext
    )
  }

  fun copy(...): Builder {...}

  fun build(): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        requestedDispatcher = requestedDispatcher,
        interceptors = interceptors,
        executionContext = executionContext,
    )
  }
}
```

💡 Note: the primary constructor that exposes all the fields is `private`, it is only used by the `copy` method. Users can use either of the 2 other constructors.

### Updates to `ApolloClient`

- Delete short hand constructor
- Make main constructor `private`, and remove parameters' default values (`@JvmOverloads` can also be removed)
- Delete with-ers:
    - `withCustomScalarAdapter`
    - `withInterceptor`
    - `withFlowDecorator`
    - `withExecutionContext`
- Make the `copy` fun `private`
    - Note: `withExecutionContext` is using `copy` which is why we can't just delete it. We could instead make `executionContext` a var with a `private set`, and get rid of `copy`?
- Make `networkTransport`  `private` (⚠️ not sure about this one - searching for usage seems to indicate it can be safely done, but this may break outside code?)

### About `ExecutionParameters`

Currently this interface is implemented by `ApolloClient` and `ApolloRequest` :

```kotlin
interface ExecutionParameters<T> where T : ExecutionParameters<T> {
  val executionContext: ExecutionContext
  fun withExecutionContext(executionContext: ExecutionContext): T
}
```

It will now be implemented by the Builders - but we have to keep `ApolloClient` and `ApolloRequest` implementing it, as the `executionContext` val is used in several extensions (e.g., `ExecutionParameters<T>.httpMethod()`)

**Suggestion**: we should probably rename `withExecutionContext` to `addExecutionContext` for consistency - WDYT?

🤔 Maybe we could split this interface in 2:

- One with only the `val executionContext` (read-only, for `ApolloClient` and `ApolloRequest`)
- One with the mutation method (for the Builders)

If we want to go ahead with this, it should done as a separate task, to keep this refactoring manageable.

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
    val operation: Operation<D>,
    val requestUuid: Uuid = uuid4(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty, // <- not sure if this parameter is useful?
) : ExecutionParameters<Builder<D>> {
  
  override fun withExecutionContext(executionContext: ExecutionContext): Builder<D> {
    return copy(executionContext = this.executionContext + executionContext)
  }
  
  fun copy(...): Builder {...}
  
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

## Open questions

- After this refact, it will still be possible to call `ExecutionParameters` extensions directly on `ApolloClient` and `ApolloRequest`, which is a bit inconsistent. Instead, would it make more sense to implement `buildUpon()` or `builder()` methods that return a `Builder` on which these methods should be called?

For instance, an interceptor wanting to add custom headers would look like:

```kotlin
class MyInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    chain.proceed(
      request
        .buildUpon()
        .httpHeader("myHeader", "value")
        .build()
    )
  }
}
```