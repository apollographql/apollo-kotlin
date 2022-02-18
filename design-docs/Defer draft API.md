# Apollo Kotlin: @defer draft API

This document outlines proposed changes to the user-facing API of Apollo Kotlin related to supporting the new `@defer`
directive.

Implementation is not discussed for now, the idea is to first agree on the desired API, other documents will dive into
implementation.

>
> 💡 Note: even though both `@defer` and `@stream` are part of the same spec, it has been decided to focus on `@defer`
> first, as it appears `@stream` has a lower demand.
>

## Context

- Proposed GraphQL spec changes: https://github.com/graphql/graphql-spec/pull/742
- Original RFC: https://github.com/graphql/graphql-wg/blob/main/rfcs/DeferStream.md
- Working Group: https://github.com/robrichard/defer-stream-wg

This directive applies to fragment spreads and inline fragments. When present, the fragment’s fields can be received
incrementally.

For example:

```graphql
type Query {
  computers: [Computer!]!
}

type Computer {
  id: ID!
  cpu: String!
  year: Int!
  screen: Screen!
}

type Screen {
  resolution: String!
  isColor: Boolean!
}
```

```graphql
query Query {
  computers {
    id
    ... on Computer @defer {
      cpu
      year
      screen {
        resolution
        ... on Screen @defer {
          isColor
        }
      }
    }
  }
}
```

Would be received as:

```json
// Payload 1
{
  "data": {
    "computers": [
      {
        "id": "Computer1"
      },
      {
        "id": "Computer2"
      }
    ]
  },
  "hasNext": true
}

// Payload 2
{
  "data": {
    "cpu": "386",
    "year": 1993,
    "screen": {
      "resolution": "640x480"
    }
  },
  "path": [
    "computers",
    0
  ],
  "hasNext": true
}

// Payload 3
{
  "data": {
    "cpu": "486",
    "year": 1996,
    "screen": {
      "resolution": "800x600"
    }
  },
  "path": [
    "computers",
    1
  ],
  "hasNext": true
}

// Payload 4
{
  "data": {
    "isColor": false
  },
  "path": [
    "computers",
    0,
    "screen"
  ],
  "hasNext": true
}

// Payload 5 (final)
{
  "data": {
    "isColor": false
  },
  "path": [
    "computers",
    1,
    "screen"
  ],
  "hasNext": false
}
```

## User facing API changes

### Generated models

**Response based**

Not supported at first. Like for `@skip` / `@include`, compilation will fail if `@defer` is used on fragments when using
response based codegen.

**Operation based**

In the generated Data models, the synthetic fields for fragments will be nullable (like currently with polymorphism or
when using `@skip` / `@include`)

So in the example above the generated models will look like:

```kotlin
data class Computer(
  val id: String,
  val computerFields: ComputerFields? // <-- nullable
)

data class ComputerFields(
  val cpu: String,
  val year: Int,
  val screen: Screen
)

data class Screen(
  val resolution: String,
  val screenFields: ScreenFields? // <-- nullable
)

data class ScreenFields(
  val isColor: Boolean
)
```

### Operation execution

**Incremental emissions**

The current API already has a `Flow` based method, `fun ApolloCall.toFlow(): Flow<ApolloResponse<D>>`, which is used like this:

```kotlin
apolloClient.query(Query()).toFlow().collect { response ->
  // ...
}
```

Currently this returns a Flow with only 1 emitted element (or 2 with the recently added `FetchPolicy.CacheAndNetwork`).

With this proposed change, it will return 1 initial response plus any subsequent responses, in an incremental manner.

For each subsequent payload, a Data instance is incrementally built by combining it with the previous one.

So in the example above, a collector would receive:

```kotlin
// Emission 1
Data(
    computers=[
        Computer(
            id=Computer1,
            computerFields=null
        ),
        Computer(
            id=Computer2,
            computerFields=null
        )
    ]
)

// Emission 2
Data(
    computers=[
        Computer(
            id=Computer1,
            computerFields=ComputerFields(
                cpu=386,
                year=1993,
                screen=Screen(
                    resolution=640x480,
                    screenFields=null
                )
            )
        ),
        Computer(
            id=Computer2,
            computerFields=null
        )
    ]
)

// Emission 3
Data(
    computers=[
        Computer(
            id=Computer1,
            computerFields=ComputerFields(
                cpu=386,
                year=1993,
                screen=Screen(
                    resolution=640x480,
                    screenFields=null
                )
            )
        ),
        Computer(
            id=Computer2,
            computerFields=ComputerFields(
                cpu=486,
                year=1996,
                screen=Screen(
                    resolution=800x600,
                    screenFields=null
                )
            )
        )
    ]
)

// Emission 4
Data(
    computers=[
        Computer(
            id=Computer1,
            computerFields=ComputerFields(
                cpu=386,
                year=1993,
                screen=Screen(
                    resolution=640x480,
                    screenFields=ScreenFields(
                        isColor=false
                    )
                )
            )
        ),
        Computer(
            id=Computer2,
            computerFields=ComputerFields(
                cpu=486,
                year=1996,
                screen=Screen(
                    resolution=800x600,
                    screenFields=null
                )
            )
        )
    ]
)

// Emission 5 (final and full)
Data(
    computers=[
        Computer(
            id=Computer1,
            computerFields=ComputerFields(
                cpu=386,
                year=1993,
                screen=Screen(
                    resolution=640x480,
                    screenFields=ScreenFields(
                        isColor=false
                    )
                )
            )
        ),
        Computer(
            id=Computer2,
            computerFields=ComputerFields(
                cpu=486,
                year=1996,
                screen=Screen(
                    resolution=800x600,
                    screenFields=ScreenFields(
                        isColor=false
                    )
                )
            )
        )
    ]
)
```

Note: this will work the same for subscriptions (which are already consumed through the `ApolloCall.toFlow()` API).
The difference is now collectors will possibly receive “partial” (not fully formed, with some fragments temporarily
`null`) data in the stream.

**Limitations/caveats**

With this approach, the incrementally built data is visible to the user, but not the individual parts.

For instance, we’ll get:

- A Computer
- A Computer, with computerFields
- A Computer, with computerFields, with screenFields

We won’t get:

- A Computer
- The computerFields
- The screenFields

In a way the API is “high level”, it hides the details of the network payloads to the user.

- Pros:
    - Users don’t need to manually combine the parts
    - The current API (exposing a `Flow<Query.Data>`) can be used
- Cons:
    - In some scenarios the individual parts may be useful?

Another limitation of this approach is that, while receiving partial data, users don’t have a way to know if a fragment
is `null` temporarily (because its fields were not yet received), or permanently (because the type of the object doesn’t
match the fragment’s type condition).

The ability to make this distinction could be useful to display either a loading or empty state in the UI while the
results are being received. I suspect this may be rarely an issue however.

💡 We could overcome this by using a sealed class e.g. `Loading`/ `Loaded` / `Absent` instead of nullability for
fragment fields.

**Combined single emission**

Calling `suspend fun ApolloCall.execute(): ApolloResponse<D>` will wait for all the incremental payloads to be received
and return the fully combined data in the response.

This allows users to ignore the incremental nature of the response if desired.

**Error handling**

Errors can be present in the initial payload as well as in any subsequent payloads.

This will be reflected by emitting the `ApolloResult` with the `errors` field combining the received errors.

So for instance when receiving this:

```json
// Payload 1
{
  "data": {
    "computers": [
      {
        "id": "Computer1"
      },
      {
        "id": "Computer2"
      }
    ]
  },
  "hasNext": true
}

// Payload 2
{
  "data": {
    "cpu": "386",
    "year": 1993,
    "screen": {
      "resolution": "640x480"
    }
  },
  "path": [
    "computers",
    0
  ],
  "hasNext": true
}

// Payload 3
{
  "data": {
    "cpu": "486",
    "year": 1996,
    "screen": {
      "resolution": "800x600"
    }
  },
  "path": [
    "computers",
    1
  ],
  "hasNext": true
}

// Payload 4
{
  "data": null,
  "path": [
    "computers",
    0,
    "screen"
  ],
  "errors": [
    {
      "message": "Cannot resolve isColor",
      "locations": [
        {
          "line": 12,
          "column": 11
        }
      ],
      "path": [
        "computers",
        0,
        "screen",
        "isColor"
      ]
    }
  ],
  "hasNext": true
}

// Payload 5 (final)
{
  "data": null,
  "path": [
    "computers",
    1,
    "screen"
  ],
  "errors": [
    {
      "message": "Cannot resolve isColor",
      "locations": [
        {
          "line": 12,
          "column": 11
        }
      ],
      "path": [
        "computers",
        1,
        "screen",
        "isColor"
      ]
    }
  ],
  "hasNext": false
}
```

⚠️ Notice how a `data` can be null on a `screen` field that is of the non-nullable type `Screen!` - more info about
this [here](https://github.com/robrichard/defer-stream-wg/discussions/23).

In this example, a collector would receive:

```kotlin
// Emission 1
Response(
    errors=null,
    data=Data(
        computers=[
            Computer(
                id=Computer1,
                computerFields=null
            ),
            Computer(
                id=Computer2,
                computerFields=null
            )
        ]
    )
)

// Emission 2
Response(
    errors=null,
    data=Data(
        computers=[
            Computer(
                id=Computer1,
                computerFields=ComputerFields(
                    cpu=386,
                    year=1993,
                    screen=Screen(
                        resolution=640x480,
                        screenFields=null
                    )
                )
            ),
            Computer(
                id=Computer2,
                computerFields=null
            )
        ]
    )
)

// Emission 3
Response(
    errors=null,
    data=Data(
        computers=[
            Computer(
                id=Computer1,
                computerFields=ComputerFields(
                    cpu=386,
                    year=1993,
                    screen=Screen(
                        resolution=640x480,
                        screenFields=null
                    )
                )
            ),
            Computer(
                id=Computer2,
                computerFields=ComputerFields(
                    cpu=486,
                    year=1996,
                    screen=Screen(
                        resolution=800x600,
                        screenFields=null
                    )
                )
            )
        ]
    )
)    

// Emission 4
Response(
    errors=[
        Error(
            message = Cannot resolve isColor,
            locations = [Location(line = 12, column = 11)],
            path=[computers, 0, screen, isColor],
            extensions = null,
            nonStandardFields = null
        )
    ],
    data=Data(
        computers=[
            Computer(
                id=Computer1,
                computerFields=ComputerFields(
                    cpu=386,
                    year=1993,
                    screen=Screen(
                        resolution=640x480,
                        screenFields=null
                    )
                )
            ),
            Computer(
                id=Computer2,
                computerFields=ComputerFields(
                    cpu=486,
                    year=1996,
                    screen=Screen(
                        resolution=800x600,
                        screenFields=null
                    )
                )
            )
        ]
    )
)    

// Emission 5 (final and full)
Response(
    errors=[
        Error(
            message = Cannot resolve isColor,
            locations = [Location(line = 12, column = 11)],
            path=[computers, 0, screen, isColor],
            extensions = null,
            nonStandardFields = null
        ),
        Error(
            message = Cannot resolve isColor,
            locations = [Location(line = 12, column = 11)],
            path=[computers, 1, screen, isColor],
            extensions = null,
            nonStandardFields = null
        )
    ],
    data=Data(
        computers=[
            Computer(
                id=Computer1,
                computerFields=ComputerFields(
                    cpu=386,
                    year=1993,
                    screen=Screen(
                        resolution=640x480,
                        screenFields=null
                    )
                )
            ),
            Computer(
                id=Computer2,
                computerFields=ComputerFields(
                    cpu=486,
                    year=1996,
                    screen=Screen(
                        resolution=800x600,
                        screenFields=null
                    )
                )
            )
        ]
    )
)
```

**Extensions**

Similarly to `errors`, an `extensions` field can be present in the initial and any subsequent payloads, which will be
reflected by merging them. The value of `extensions` is a map, and the same key can be present in several payloads. In
that case it can be reflected by using a list as the value:

```jsx
// Payload 1:
extensions: { "a": 1, "foo": "bar" }

// Payload 2:
extensions: { "foo": "baz" }
```

Would be received as:

```kotlin
// Emission 1:
extensions = { a = 1, foo = bar }

// Emission 2:
extensions = { a = 1, foo = [bar, baz] }
```

**Impacts on ApolloInterceptor**

Some implementations of `ApolloInterceptor` may assume that proceeding down the chain will return a single value,
because that is always the case currently (except when using the normalized cache). Emitting several values in the Flow
will break such implementations.

### HttpEngine / HttpInterceptor / HttpResponse

Here’s the current definitions of these 3 classes:

```kotlin
interface HttpEngine {
  suspend fun execute(request: HttpRequest): HttpResponse
  fun dispose()
}

interface HttpInterceptor {
  suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse
  fun dispose() {}
}

class HttpResponse {
  val statusCode: Int,
  val headers: List<HttpHeader>,
  val body: BufferedSource?,
}
```

As we can see the contracts are designed around 1 full response being returned from a request, whereas when
using `@defer` this is no longer appropriate: several parts can be returned.

To address this let’s augment `HttpResponse` with a Flow representing the parts:

```kotlin
class HttpResponse {
  val statusCode: Int,
  val headers: List<HttpHeader>,
  val body: BufferedSource?,
  val parts: Flow<HttpPart>?, // <- New!
}

class HttpPart {
  val headers: List<HttpHeader>,
  val body: BufferedSource,
}
```

This introduces `HttpPart` which is a light version of `HttpResponse`.

Notes:

- in the non-multipart (`@defer` not used) case, `parts` will be `null`
- and conversely, when `parts` is present, `body` is `null`
- any `HttpInterceptor` implementation that manipulates the response body won’t automatically work on parts. Not sure
  if this is a common use-case but it will break / not work as expected.

### Http Cache

The http cache can still be used in `@defer` scenarios.

A response will be cached after all its parts have been received.

When a response comes from the cache, the parts will be available immediately.

### Batching

At this point it is unclear whether batching in a `@defer` scenario is useful. For now, we can decide that batching is
automatically opted-out for any operations using `@defer`.

### Normalized Cache

**The data is stored in the cache as it is being received.**

This means:

- Querying the cache for data for which a query is ongoing and some fields haven't been received yet, will
  lead to a cache miss.
- Same is true when using `watch(Data?)`: only data where all fields have been received will be emitted.
- Similarly, reading fragments from the store will either throw or not, depending on if the fields for this fragment
  has been received.
- An API to watch fragments would be useful in a `@defer` scenario:
  - different UI components are watching for different fragments
  - they are notified as soon as the fragment's fields are received

Essentially, `@defer` is ignored by the cache. 

Because of this, we could throw an exception when a query with `@defer` is executed only on the
cache (`FetchPolicy.CacheOnly`, or with `watch(Data?)`) because it doesn't make much sense.

- Pros: prevents some confusion
- Cons: can lead to code duplication (1 query with `@defer` for cache+network, and the same without `@defer` for cache
  only/watch)

**Key fields in deferred fragments**

What happens if all or some of the key fields are deferred on a query? For example:

```graphql
query Query {
  computers {
    cpu
    year
    ... on Computer @defer {
      id
    }
  }
}
```

**Option 1:** keep the current behavior.

Currently, id fields are automatically added to selection sets, so for instance the above query will be transformed to:

```graphql
query Query {
  computers {
    id
    cpu
    year
    ... on Computer @defer {
      id
    }
  }
}
```
This means that an object, even partial, can always be stored in the cache, because its id fields are available.

However, this may not be what the user expects/wants: the id fields may be part of fields that take longer to load
and are not needed at first. Adding them to the initial payloads may mean that initial payloads won't arrive as fast
as they could.

On the other hand, we shouldn't expect this to be a real problem *in most cases*: ids usually correspond to
primary keys, which aren't fields that "take long to load" on the server.

💡 Maybe this could output a warning? But ideally we would need a way to suppress it.

**Option 2:** update current behavior: don't add key fields on selection sets if they are already part of a `@defer`
  fragment.

The reasoning is that we don't want to add id fields in initial payloads if the user wants them to be deferred.

In that case we may store partial objects in the cache only if all the key fields are available.

In the example above, for instance, we would receive the following objects:

```kotlin
// Emission 1
Data(
        computers=[
          Computer(
                  cpu=386,
                  year=1993,
                  computerFields=null
          )
        ]
)

// Emission 2
Data(
        computers=[
          Computer(
                  cpu=386,
                  year=1993,
                  computerFields=ComputerFields(
                          id=Computer1
                  )
          ),
        ]
)
```
The first emission would not update the cache (id not available), and the second one would.

The drawback is that observers of the cache wouldn't get updated as early as data is available (UI is less reactive).


**Option 3:** detect and disallow such case

Pros:

- Queries are not automatically modified, removing the risk of "accidentally not deferring" fields that should be
  deferred.
- The user is in charge.

Cons:
- This may be confusing.
- The most probable resolution is to manually do what is currently done automatically (adding the id fields to the root
  selection set).


All in all, option 1 seems to be acceptable at least for the first milestone, and has the advantage of being
already in place.

### Mock Server

Mock Server’s API will be augmented with the ability to enqueue responses with chunks.

To do this, similarly to `HttpResponse`, let’s add a `chunks` field to `MockResponse`:

```kotlin
class MockResponse(
    val statusCode: Int = 200,
    val body: ByteString = ByteString.EMPTY,
    val headers: Map<String, String> = emptyMap(),
    val delayMillis: Long = 0,
    val chunks: List<MockChunk> = emptyList(), // <- New!
)
```

`MockChunk` is identical to `HttpChunk` but with a `delayMills` field (similarly to `MockResponse` vs `HttpResponse`).
