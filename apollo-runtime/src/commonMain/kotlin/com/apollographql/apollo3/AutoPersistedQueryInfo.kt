package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation

/**
 * Information about auto persisted queries
 *
 * @param hit: whether the query was hit or required another round trip to send the document
 */
class AutoPersistedQueryInfo(
    val hit: Boolean,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<AutoPersistedQueryInfo>
}

val <D : Operation.Data> ApolloResponse<D>.autoPersistedQueryInfo
  get() = executionContext[AutoPersistedQueryInfo]

