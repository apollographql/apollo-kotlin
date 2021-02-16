package com.apollographql.apollo3.api

/**
 * Represents a GraphQL mutation operation that will be sent to the server.
 */
interface Mutation<D : Operation.Data> : Operation<D>
