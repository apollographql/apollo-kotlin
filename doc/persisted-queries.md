# Persisted queries

## Automatic persisted queries

Apollo supports [Automatic Persisted Queries](https://www.apollographql.com/docs/apollo-server/performance/apq/).

// TODO: add some sample code

## Transformed queries

If your backend uses custom persisted queries, Apollo-Android can generate an operationOutput json that describes the different operations.

```
apollo {
  generateOperationOutput = true
}
```