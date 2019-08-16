package com.apollographql.apollo.api

/**
 * Represents a GraphQL subscription.
 */
interface Subscription<D : Operation.Data, T, V : Operation.Variables> : Operation<D, T, V>
