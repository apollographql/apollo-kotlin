package com.apollographql.apollo3.api

/**
 * Represents a GraphQL subscription.
 */
interface Subscription<D : Subscription.Data> : Operation<D> {
  interface Data: Operation.Data
}
