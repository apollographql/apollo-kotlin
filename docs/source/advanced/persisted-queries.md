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

If your backend uses custom persisted queries, Apollo-Android can generate an OperationOutput json from your .graphql queries. They will match what the client is sending exactly so you can persist them on your server.

```
apollo {
  generateOperationOutput = true
}
```
