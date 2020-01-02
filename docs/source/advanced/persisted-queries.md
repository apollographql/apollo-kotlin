---
title: Persisted queries 
---

## Automatic persisted queries

Apollo Android supports [Automatic Persisted Queries](https://www.apollographql.com/docs/apollo-server/performance/apq/).
Your server needs to supports it (apollo-server supports it out of the box).
Enable the feature in apollo-android like so:

```java
ApolloClient.builder()
  /* ... */
  .enableAutoPersistedQueries(true)
  /* ... */
  .build()
```

You can optionally configure apollo-android to send GET HTTP verbs for queries, to benefit from caching if your server uses a CDN:
```java
ApolloClient.builder()
  /* ... */
  .enableAutoPersistedQueries(true)
  .useHttpGetMethodForQueries(true)
  /* ... */
  .build()
```


## Transformed queries

If your backend uses custom persisted queries, Apollo-Android can generate transformed queries from your .graphql queries. They will match what the client is sending exactly so you can persist them on your server.

```
apollo {
  generateTransformedQueries = true
}
```

## Custom ID for Persisted Queries

By default, Apollo uses `Sha256` hashing algorithm to generate an ID for the query. To provide a custom ID generation logic, use the option - `customIdGenerator` which accepts an `instance` that implements the `CustomIdGenerator` interface (`com.apollographql.apollo.compiler.CustomIdGenerator`) as the input. This option can be used to either specify a different Hashing Algorithm or to fetch the persisted query id from a different place - e.g. a service or a CLI.

Example Md5 hash generator:

```groovy
import com.apollographql.apollo.compiler.CustomIdGenerator

apollo {
  customIdGenerator = new CustomIdGenerator() {
    String apply(String queryString, String queryFilepath) {
      return queryString.md5()
    }
    String version = "v1"
  } 
}
```
