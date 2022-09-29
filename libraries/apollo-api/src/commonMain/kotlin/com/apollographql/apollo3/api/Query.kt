package com.apollographql.apollo3.api

/**
 * Represents a GraphQL query that will be sent to the server.
 */
interface Query<D : Query.Data> : Operation<D> {
  interface Data: Operation.Data
}
