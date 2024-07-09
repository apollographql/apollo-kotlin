package com.apollographql.apollo.api

/**
 * Type safe representation of a [GraphQL subscription](https://spec.graphql.org/October2021/#sec-Subscription).
 */
interface Subscription<D : Subscription.Data> : Operation<D> {
  interface Data: Operation.Data
}
