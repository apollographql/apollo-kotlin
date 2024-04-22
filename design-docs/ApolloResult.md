> [!Warning]
> This document is work in progress and meant as a record/central place to gather discussions happening on the topic.

# Modeling GraphQL results in Kotlin

Data modeling and error handling is a complex topic, in general but in GraphQL even more.

GraphQL may have field errors, request errors, HTTP errors, I/O errors and more. When it comes to subscriptions and watchers, some errors may be terminal while other may not. What errors should be retried, what errors cannot be recovered, what errors can be ignored?

Throw in caching and nullability in the mix, and you get nice explosion of different error states which are quite hard to track for the non-expert client developer.

Historically, the Apollo Kotlin API was using callbacks. With Apollo Kotlin 3 and Kotlin coroutines, errors were modelled as exceptions in order to leverage the `Flow` operators. The general idea was that the normal case would produce a (maybe partial) response and the exceptional case would throw. Sounded reasonable at the time but unfortunately using exceptions has some issues:

- Two different ways to handle errors make it difficult to handle errors at the same area of the code.
- Exceptions must be caught, or they will crash your application. They bypass the type system.
- Exceptions are terminal in Flows:
  - TODO: find and link GitHub issues illustrating issues the issues 

This page documents the various tradeoffs and decisions taken so far and explores ways the API could evolve in the future.

## Current state

`ApolloResponse` contains GraphQL error(s) and potential exception. This was made as a minimal change but has a drawback that impossible states are allowed by the type system:

```kotlin
ApolloResponse(
    data = SomeData(),
    // Not possible but the type system allows it
    exception = IOException()
)
```

Cache misses are modeled as exceptions. Again, the main advantage is simplicity but this does not allow partial cached data:

```json
{
  "data": {
    "cachedField": "value0",
    "missingField": null
  },
  "errors": [
    {
      "message": "Cache miss on missingField",
      "path": ["missingField"]
    }
  ]
}
```

Subscriptions and watchers reuse the same `ApolloResponse` class as well as interceptors. There is a `Response.isLast` field whose semantics are a bit ill-defined and that was introduced in order to provide a guarantee that watchers subscribe to the store before the last fetch item is delivered downstream (see https://github.com/apollographql/apollo-kotlin/pull/3853). 
* Whether a subscription should be retried or a given error ignored is left to the caller.
* Similarly, it's not possible to insert metadata event such as "connection is established" that would allow displaying a status indicator.

## Future state (?)

We _could_ introduce `ApolloEvent` and `ApolloResult` (WIP):

```kotlin
sealed interface ApolloEvent

sealed interface ApolloResult<D: Operation.Data>: ApolloEvent {
    val requestUuid: UUID
    val operation: Operation<D>
    val executionContext: ExecutionContext
}

/**
 * A GraphQL response was received, possibly partial
 */
class ApolloResponse<D: Operation.Data>(
    val data: D?,
    val errors: List<Error>?,
    val extensions: Map<String, Any?>
    // Do we need isLast here?
    // val isLast: Boolean
) : ApolloResult<D>

/**
 * A GraphQL response was not received
 */
class ApolloError<D: Operation.Data>(
    val exception: ApolloException
): ApolloResult<D>

/**
 * The status of the subscription changed
 */
class ApolloStatus<D: Operation.Data>(
    val connected: Boolean
): ApolloEvent

fun <D: Operation.Data> ApolloQueryCall.execute(): ApolloResult<D>
fun <D: Operation.Data> ApolloMutationCall.execute(): ApolloResult<D>

fun <D: Operation.Data> ApolloSubscriptionCall.toFLow(): Flow<ApolloEvent>
```

The impact to the callsites still has to be evaluated though. 

## The meaning of `ApolloResponse.isFromCache`

In v3, `ApolloResponse.isFromCache` returns `true` if the **data** comes from the cache (implementation:
`cacheInfo?.isCacheHit == true`). A more descriptive name could have been `isCacheHit`.

In v4, since cache misses no longer throw, a response can come from the cache whether the data is present or not.

With that in mind, it makes more sense for `isFromCache` to return `true` if the **response** comes from the cache,
regardless of the data being present or not (new implementation:
`cacheInfo?.isCacheHit == true || exception is CacheMissException`).

Note: this **is** a behavior change for projects that used `emitCacheMisses(true)` in v3:

- cache miss responses in v3: `isFromCache` would return `false` ("data is not a cache hit")
- cache miss responses in v4: `isFromCache` will return `true` ("response is from the cache")

More context in [#5799](https://github.com/apollographql/apollo-kotlin/issues/5799).

## Terminology

**Field error**: A GraphQL field error [as in the GraphQL spec](https://spec.graphql.org/draft/#field-error). Typically, a backend resolver fails.

**Request error**: A GraphQL request error [as in the GraphQL spec](https://spec.graphql.org/draft/#request-error). Typically, a query fails validation.

**GraphQL error**: Any GraphQL error returned by the server in `response.errors`. Can be either a field error or a request error.

**Response**: A GraphQL response [as in the GraphQL spec](https://spec.graphql.org/draft/#sec-Response-Format). Typically in JSON format.

**Exception**: A Java/Kotlin Exception. Exceptions contain the stacktrace where they happened. Because they need to capture a stacktrace, they also have an associated cost and shouldn't be used for flow control.

## Links

* https://elizarov.medium.com/kotlin-and-exceptions-8062f589d07
* https://blog.joda.org/2010/09/checked-exceptions-bijava_9688.html
* https://doc.rust-lang.org/book/ch09-00-error-handling.html
* https://github.com/skydoves/sandwich
* https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/
