# `@defer`: merging implementation

With `@defer`, we receive data in parts, which need to be merged before they're surfaced to the user.

This document explores the implementation of this area of the `@defer` support. 

We'll use the same example as in the [Draft API doc](Defer draft API.md):

**Schema:**

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

**Query:**

```graphql
query Query1 {
    computers {
        id
        ...ComputerFields @defer
    }
}

fragment ComputerFields on Computer {
    cpu
    year
    screen {
        resolution
        ...ScreenFields @defer
    }
}

fragment ScreenFields on Screen {
    isColor
}

```

**Received payloads:**

```json
// Payload 1
{"data":{"computers":[{"id":"Computer1"},{"id":"Computer2"}]},"hasNext":true}


// Payload 2
{"data":{"cpu":"386","year":1993,"screen":{"resolution":"640x480"}},"path":["computers",0],"hasNext":true}


// Payload 3
{"data":{"cpu":"486","year":1996,"screen":{"resolution":"640x480"}},"path":["computers",1],"hasNext":true}


// Payload 4
{"data":{"isColor":false},"path":["computers",0,"screen"],"hasNext":true}


// Payload 5
{"data":{"isColor":false},"path":["computers",1,"screen"],"hasNext":false}

```

### Approach A: Parse JSON payloads, then merge objects

With this approach, we use the generated Adapter for the `Query.Data` on the initial payload, and then the Adapters for
the appropriate Fragments on the subsequent payloads.

The results of parsing the JSON payloads using these adapters are then merged with the initial data, which is emitted
at each step.

Inside HttpNetworkTransport (Pseudocode):

```kotlin
var data
while (true) {
  val payload = receivePayload()  
  if (payloadIndex == 0) {
    // Initial payload
    data = parse(payload, operation.adapter())
  } else {
    // Subsequent payloads
    // 1. find appropriate adapter 
    val fragmentAdapter = operation.getFragmentAdapter(payload)
    // 2. parse payload with it
    val fragment = parse(payload, fragmentAdapter)
    // 3. merge with data
    data = operation.merge(data, fragment)
  }
  emit(data)
  
  if (!payload.hasNext) break
  payloadIndex++
}

```

In our example,

- Payload 1 should be parsed using `Query_ResponseAdapter.Data: Adapter<Query.Data>`
- Payloads 2 and 3 should be parsed using `ComputerFieldsImpl_ResponseAdapter.ComputerFields : Adapter<ComputerFields>`
- Payloads 4 and 5 should be parsed using `ScreenFieldsImpl_ResponseAdapter.ScreenFields : Adapter<ScreenFields>`

#### Adapters must not parse deferred fragments

When parsing payload 1, the fields of the `ComputerFields` fragment are not present, so the adapter should not try to
read them.

The same is true when parsing Payload 2 and 3: the fields for `ScreenFields` are not present, so the adapter should not
try to read them.

Solution:

- In the generated Adapter code, do not output the code to parse fragments that are deferred

For instance currently the generated code for `ComputerFieldsImpl_ResponseAdapter.ComputerFields` looks like this:

```kotlin
reader.rewind()
val screenFields = com.example.fragment.ScreenFieldsImpl_ResponseAdapter.ScreenFields.fromJson(reader,
    customScalarAdapters)

return com.example.fragment.ComputerFields.Screen(
    __typename = __typename,
    resolution = resolution!!,
    screenFields = screenFields
)
```

Instead, we'd have:

```kotlin
reader.rewind()
val screenFields = null

return com.example.fragment.ComputerFields.Screen(
    __typename = __typename,
    resolution = resolution!!,
    screenFields = screenFields
)
```

#### Parsing deferred payloads

For each received deferred payload, the appropriate Fragment Adapter must be determined and used.

- The `@defer` directive has an optional `label` parameter which is returned in the
  payloads.
- The codegen will automatically add a value for `label` (unless already present). The label doesn't have to be meaningful but needs to be unique per operation. An incrementing Int prefixed
  by the operation or fragment where the directive is used should do the job (e.g. `query:Query1:0`,
  `fragment:ComputerFields:0`, etc.).
- The codegen can generate a utility used to get the appropriate Fragment Adapter in function of the label.

The generated code should look something like this:

```kotlin
object DeferFragmentAdapterMap {
    fun getFragmentAdapter(label: String): Adapter<*> {
        return when (label) {
            "query:Query1:0" -> com.example.fragment.ComputerFieldsImpl_ResponseAdapter.ComputerFields
            "fragment:ComputerFields:0" -> com.example.fragment.ScreenFieldsImpl_ResponseAdapter.ScreenFields

            else -> throw IllegalArgumentException("Unknown label: $label")
        }
    }
}
```

#### Merge the data classes

Using the generated code above, from the payload, we get these data classes in that order:

```kotlin
// Payload 1
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

// Payload 2
ComputerFields(
    cpu=386,
    year=1993,
    screen=Screen(
        resolution=640x480
    )
)

// Payload 3
ComputerFields(
    cpu=486,
    year=1996,
    screen=Screen(
        resolution=640x480
    )
)

// Payload 4
ScreenFields(
    isColor=false
)

// Payload 5
ScreenFields(
    isColor=false
)

```

We need to incrementally incorporate these data classes into the Data that is surfaced to the client.

To do this we would need to generate code that would look something like this:

```kotlin

object Query1Merger {

    fun merge(data: Query1Query.Data, fragment: Any, label: String, path: List<Any>): Query1Query.Data {
        return when (label) {
            "query:Query1:0" -> {
                val computerFields = fragment as com.example.fragment.ComputerFields
                
                // Fragment to replace is at data/computers/$index/computerFields
                val computersIndex = path[1] as Int
                val toReplace = data.computers[computersIndex]
                val replaced = toReplace.withComputerFields(computerFields)
                data.copy(
                    computers = data.computers.replace(computersIndex, replaced)
                )
            }

            "fragment:ComputerFields:0" -> {
                val screenFields = fragment as com.example.fragment.ScreenFields

                // Fragment to replace is at data/computers/$index/computerFields/screen/screenFields
                val computersIndex = path[1] as Int
                val toReplace = data.computers[computersIndex].computerFields!!.screen
                val replaced = toReplace.withScreenFields(screenFields)
                data.copy(
                    computers = data.computers.replace(computersIndex, data.computers[computersIndex].copy(
                        computerFields = data.computers[computersIndex].computerFields!!.copy(
                            screen = replaced
                        )
                    ))
                )
            }

            else -> throw IllegalArgumentException("Unknown label: $label")
        }
    }

    fun Query1Query.Computer.withComputerFields(computerFields: com.example.fragment.ComputerFields): Query1Query.Computer {
        return this.copy(computerFields = computerFields)
    }

    fun ComputerFields.Screen.withScreenFields(screenFields: com.example.fragment.ScreenFields): ComputerFields.Screen {
        return this.copy(screenFields = screenFields)
    }

    fun <T> List<T>.replace(index: Int, element: T): List<T> = toMutableList().apply { set(index, element) }
    
}

```

⚠️ To me, such a code seems particularly difficult to generate:
- getting the `toReplace` object, from its path in the data, but also combining it with the `path` argument (for arrays)
- the complex expression with several `copy` calls due to data classes


### Approach B: Merge JSON payloads, and parse that

In this approach, the merging is done at the JSON level.

A version of the whole JSON payload is kept in memory, and patched each time a part is received.

The patched JSON is fed into the Adapter for the operation at each step, and the resulting object is emitted.

Inside HttpNetworkTransport (Pseudocode):

```kotlin
var wholeJson
while (true) {
  val payload = receivePayload()
  wholeJson = mergeJson(wholeJson, payload)
  data = parse(wholeJson, operation.adapter())
  emit(data)
  
  if (!payload.hasNext) break
}

```

#### Merging JSON

We need a utility function that is able to merge a JSON document into another JSON document, given a path.

For instance, the following JSON (initial payload 1):

```json
{
  "computers": [
    {
      "__typename": "Computer",
      "id": "Computer1"
    },
    {
      "__typename": "Computer",
      "id": "Computer2"
    }
  ]
}
```

Merged with the following JSON (payload 2):

```json
{
  "cpu": "386",
  "year": 1993,
  "screen": {
    "resolution": "640x480"
  }
}
```

And with the path `["computers", 0]`, would result in:

```json
{
  "computers": [
    {
      "__typename": "Computer",
      "id": "Computer1",
      "cpu": "386",
      "year": 1993,
      "screen": {
        "resolution": "640x480"
      }
    },
    {
      "__typename": "Computer",
      "id": "Computer2"
    }
  ]
}
```

Converting the JSONs to `Map<String, Any?>` should make the implementation of such function not too difficult.

#### Adapters must parse deferred fragments only if present

When parsing payload 1, the fields of the `ComputerFields` fragment are not present, so the adapter should not try to
read them.

However, when parsing "payload 1 merged with payload 2", the fields for `ComputerFields` are present and must be read.

We need a way to switch this reading on/off depending on whether specific fragments have been merged or not.

Solution:

- Like with Approach A, add a unique label to each `@defer` directive.
- Add a property `deferredFragmentLabels: Set<String>` to `CustomScalarAdapters`
    - ⚠️ This is kind of a hack - we hijack the original purpose of `CustomScalarAdapters` and use it as a kind of
      context. This avoids changing the Adapter API in a breaking way.
    - We already used this technique to pass around execution variables, which are needed at parse time for `@skip`
      / `@include`.
- When handling a payload, add its label to `customScalarAdapters.deferredFragmentLabels`.
- The Adapter's generated code only parses a fragment marked with `@defer` if the associated label is present
  in `customScalarAdapters.deferredFragmentLabels`.

For instance currently the generated code for `ComputerFieldsImpl_ResponseAdapter.ComputerFields` looks like this:

```kotlin
reader.rewind()
val screenFields = com.example.fragment.ScreenFieldsImpl_ResponseAdapter.ScreenFields.fromJson(reader,
    customScalarAdapters)

return com.example.fragment.ComputerFields.Screen(
    __typename = __typename,
    resolution = resolution!!,
    screenFields = screenFields
)
```

Instead, we'd have:

```kotlin
reader.rewind()
var screenFields: ScreenFields? = null
if (customScalarAdapters.deferredFragmentLabels.contains("fragment:ComputerFields:0")) {
    screenFields = com.example.fragment.ScreenFieldsImpl_ResponseAdapter.ScreenFields.fromJson(reader,
        customScalarAdapters)
}

return com.example.fragment.ComputerFields.Screen(
    __typename = __typename,
    resolution = resolution!!,
    screenFields = screenFields
)
```

### Comparison of both approaches

Approach A:

**Pros:**
- No need to parse the JSON into Maps
- Each payload is parsed only once

**Cons:**
- Fairly complex / difficult (if even possible?) code generation to merge data classes


Approach B:

**Pros:**
- Changes to the generated Adapter are minimal

**Cons:**
- JSON payloads must be parsed into Maps, which are kept in memory for a while
- The Adapter parses the same fields several times since we feed it the whole JSON for each part

