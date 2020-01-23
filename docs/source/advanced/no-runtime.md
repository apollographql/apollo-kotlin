---
title: Using apollo without `apollo-runtime` 
---

`apollo-runtime` and `ApolloClient` provides support for doing the network requests and interacting with the cache but you can use the generated queries without the runtime if you want.

For this, remove the `com.apollographql.apollo:apollo-runtime`dependency and replace it with:

```
  implementation("com.apollographql.apollo:apollo-api:x.y.z")
```

## Parsing HTTP body

All `Operation` instances provide an API to parse `Response` from raw `okio.BufferedSource` source that represents http response body returned by the GraphQL server.
If for some reason you want to use your own network layer and don't want to use fully featured `ApolloClient` provided by `apollo-runtime` you can use this API:

```java
    okhttp3.Response httpResponse = ...;

    Response<Operation.Data> response = new Query().parse(httpResponse.body().source());
```

If you do have custom GraphQL scalar types, pass properly configured instance of `com.apollographql.apollo.response.ScalarTypeAdapters`:

```java
    okhttp3.Response httpResponse = ...;

    ScalarTypeAdapters scalarTypeAdapters = new ScalarTypeAdapters(<provide your custom scalar type adapters>);

    Response<Operation.Data> response = new Query().parse(httpResponse.body().source(), scalarTypeAdapters);
```

## Converting Query.Data back to JSON

In case you have an instance of `Operation.Data` and want to convert it back to JSON representation, you can use `OperationDataJsonSerializer.serialize` static method.

```java
    Operation.Data data = ...;

    String json = OperationDataJsonSerializer.serialize(data, "  ");
```

Just like above, you can provide instance of custom `ScalarTypeAdapters` as last argument.

Simpler extension function is available for `Kotlin` users:
```kotlin
   val json = data.toJson()

   // or
   val json = data.toJson(indent = "  ")
```

## Creating request payload for POST request

To compose a GraphQL POST request along with operation variables to be sent to the server, you can use `Operation.Variables#marshal()` API: 

```java
    // Generated GraphQL query, mutation, subscription
    Query query = ...;

    String requestPayload = "{" +
        "\"operationName\": " + query.name().name() + ", " +
        "\"query\": " + query.queryDocument() + ", " +
        "\"variables\": " + query.variables().marshal() +
        "}";
```

The same to serialize variables with the custom GraphQL scalar type adapters:

```java
    // Generated GraphQL query, mutation, subscription
    Query query = ...;
  
    ScalarTypeAdapters scalarTypeAdapters = new ScalarTypeAdapters(<provide your custom scalar type adapters>);

    String requestPayload = "{" +
        "\"operationName\": " + query.name().name() + ", " +
        "\"query\": " + query.queryDocument() + ", " +
        "\"variables\": " + query.variables().marshal(scalarTypeAdapters) +
        "}";
```
