package com.apollographql.apollo.api

/**
 * Represents a GraphQL query that will be sent to the server.
 */
interface Query<D : Operation.Data, T, V : Operation.Variables> : Operation<D, T, V>
