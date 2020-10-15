package com.apollographql.apollo.api

/**
 * Represents a GraphQL mutation operation that will be sent to the server.
 */
interface Mutation<D : Operation.Data, V : Operation.Variables> : Operation<D, V>
