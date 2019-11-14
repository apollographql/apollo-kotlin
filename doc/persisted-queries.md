# Persisted queries

## Automatic persisted queries

Apollo supports [Automatic Persisted Queries](https://www.apollographql.com/docs/apollo-server/performance/apq/).

// TODO: add some sample code

## Transformed queries

If your backend uses custom persisted queries, Apollo-Android can generate transformed queries from your .graphql queries. They will match what the client is sending exactly so you can persist them on your server.

```
apollo {
  generateTransformedQueries = true
}
```