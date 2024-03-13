package com.apollographql.apollo3.api

/**
 * Type safe representation of a [GraphQL mutation](https://spec.graphql.org/October2021/#sec-Subscription).
 */
interface Subscription<D : Subscription.Data> : Operation<D> {
  interface Data: Operation.Data
}
