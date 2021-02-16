package com.apollographql.apollo3.api

/**
 * Represents a GraphQL subscription.
 */
interface Subscription<D : Operation.Data> : Operation<D>
