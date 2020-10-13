package com.apollographql.apollo.api

import okio.ByteString

/**
 * Represents a GraphQL query that will be sent to the server.
 */
interface Query<D : Operation.Data, V : Operation.Variables> : Operation<D, V>
