# Using apollo without `apollo-runtime`

`apollo-runtime` and `ApolloClient` provides support for doing the network requests and interacting with the cache but you can use the generated queries withouth the runtime if you want.

For this, remove the `com.apollographql.apollo:apollo-runtime`dependency and replace it with:

```
  com.apollographql.apollo:apollo-api
```

All `Operation` instances provide an API to parse `Response` from raw `okio.BufferedSource` source that represents http response body returned by the GraphQL server.
If for some reason you want to use your own network layer and don't want to use fully featured `ApolloClient` provided by `apollo-runtime` you can use this API:

```java
    okhttp3.Response httpResponse = ...;

    // if you have custom scalar types, provide proper instance of ScalarTypeAdapters with your own custom adapters
    ScalarTypeAdapters scalarTypeAdapters = new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap())

    Response<Query.Data> response = new Query().parse(httpResponse.body().source(), scalarTypeAdapters);
```

Just make sure you added `apollo-api` dependency to your project's build.gradle file.
