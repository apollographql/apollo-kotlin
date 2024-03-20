package com.apollographql.apollo3.api

/**
 * Type safe representation of a [GraphQL mutation](https://graphql.org/learn/queries/#mutations).
 */
interface Mutation<D : Mutation.Data> : Operation<D> {
  interface Data: Operation.Data
}
