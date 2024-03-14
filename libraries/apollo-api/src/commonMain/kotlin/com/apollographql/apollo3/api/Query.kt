package com.apollographql.apollo3.api

/**
 * Type safe representation of a [GraphQL query](https://graphql.org/learn/queries/).
 */
interface Query<D : Query.Data> : Operation<D> {
  interface Data: Operation.Data
}
