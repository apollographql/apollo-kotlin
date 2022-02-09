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
using `@defer` this is no longer appropriate: several chunks can be returned.

To address this let’s augment `HttpResponse` with a Flow representing the chunks:

```kotlin
class HttpResponse {
  val statusCode: Int,
  val headers: List<HttpHeader>,
  val body: BufferedSource?,
  val chunks: Flow<HttpChunk>, // <- New!
}

class HttpChunk {
  val headers: List<HttpHeader>,
  val body: BufferedSource?,
}
```

This introduces `HttpChunk` which is a light version of `HttpResponse`.

Notes:

- in the non-chunked (`@defer` not used) case, `chunks` will be an empty Flow
- when `chunks` is not empty, the response `body` is `null`
- any `HttpInterceptor` implementation that manipulates the response body won’t automatically work on chunks. Not sure
  if this is a common use-case but it will break / not work as expected.

### Http Cache

The http cache can still be used in `@defer` scenarios.

A response will be cached after all its chunks have been received.

When a response comes from the cache, the chunks will be available immediately.

### Batching

At this point it is unclear whether batching in a `@defer` scenario is useful. For now, we can decide that batching is
automatically opted-out for any operations using `@defer`.

### Normalized Cache

Several options are possible:

**A. Incremental data is cached as it is being received**

Pros:

- When using `watch` the UI can be as reactive as possible and reflect the received data in real time

Cons:

- When using `query` with `FetchPolicy.CacheOnly` partial data can be returned, with no way to know this
- A potentially undesirable experience with `watch` when calls are done in parallel. For instance a collector could receive in this order:
    1. A Computer *(from request **a**)*
    2. A Computer, with computerFields *(from request **a**)*
    3. A Computer *(from request **b**)*
    4. A Computer, with computerFields, with screenFields *(from request **a**)*
    5. A Computer, with computerFields *(from request **b**)*
    6. A Computer, with computerFields, with screenFields *(from request **b**)*

Open questions

- What happens if the key fields are deferred? Maybe this could be forbidden via a compile time check?

**B. Only the full data is cached**

Similarly to the Http Cache, we wait for the last payload to be received before storing the data in the cache.

Pros:

- No risk of getting a partial data when using `query` with `FetchPolicy.CacheOnly`
- Avoids the “interweaved partial results” potential issue highlighted above

Cons:

- Less reactiveness when using `watch`
- The cache is not reflecting the latest received data
- Basically renders `@defer` useless, when using the cache as the single source of truth

**C. Same as A, but with more control**

A parameter (similarly to `storePartialResponses`) can be added on `ApolloCall` to control whether partial data is
wanted from the cache.

- Pros:
    - `watch` is “reactive” by emitting data as it’s being received, including partial, by default (but can be opted out
      if needed)
    - `query` with `FetchPolicy.CacheOnly` does not return partial data, by default (but can if needed)
- Cons:
    - Out of scope for this document but noteworthy: I don’t know how (if?) this can be implemented
    - Depending on how it is implemented, this may be a breaking change (a database migration may be needed?)

**Implementation / planning considerations**

Option C is not as simple to implement as the other two - a good way to go would probably be to implement A in the first
milestone, and implement C later on a subsequent release.

### Mock Server

Mock Server’s API will be augmented with the ability to enqueue responses with chunks.

To do this, similarly to `HttpResponse`, let’s add a `chunks` field to `MockResponse`:

```kotlin
class MockResponse(
    val statusCode: Int = 200,
    val body: ByteString = ByteString.EMPTY,
    val headers: Map<String, String> = emptyMap(),
    val delayMillis: Long = 0,
    val chunks: Flow<MockChunk> = emptyFlow {}, // <- New!
)
```

`MockChunk` is identical to `HttpChunk` but with a `delayMills` field (similarly to `MockResponse` vs `HttpResponse`).
