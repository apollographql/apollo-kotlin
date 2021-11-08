package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.sendApqExtensions
import com.apollographql.apollo3.api.http.sendDocument

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

fun <D : Operation.Data> ApolloResponse<D>.withAutoPersistedQueryInfo(hit: Boolean) = withExecutionContext(AutoPersistedQueryInfo(hit))


/**
 * A shorthand method that sets sendDocument and sendApqExtensions at the same time.
 */
fun <T> HasMutableExecutionContext<T>.hashedQuery(hashed: Boolean) where T : HasMutableExecutionContext<T> = sendDocument(!hashed).sendApqExtensions(hashed)

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHashedQuery(hashed: Boolean) = newBuilder().hashedQuery(hashed).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHashedQuery(hashed: Boolean) = newBuilder().hashedQuery(hashed).build()
