package com.apollographql.apollo3

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation

/**
 * When [conflateResponses] is true, the fetch policy interceptors emit a single response and ignores
 * the first cache or network error if a successful response is ultimately fetched.
 * If no successful response can be emitted, a [com.apollographql.apollo3.exception.ApolloCompositeException]
 * error response is emitted.
 *
 * For example, [FetchPolicy.CacheFirst] gets a cache miss, only the network response will be emitted.
 *
 * This was done so that [ApolloCall.execute] would only return a single response but this is handled
 * in a more generic way in v4
 *
 * This is provided for migration purposes only and will be removed in a future version.
 */
@Deprecated("Handle each ApolloResponse.exception instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun <T> MutableExecutionOptions<T>.conflateFetchPolicyInterceptorResponses(conflateResponses: Boolean) = addExecutionContext(
    ConflateResponsesContext(conflateResponses)
)

internal class ConflateResponsesContext(val conflateResponses: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ConflateResponsesContext>
}


val <D : Operation.Data> ApolloRequest<D>.conflateFetchPolicyInterceptorResponses
  get() = executionContext[ConflateResponsesContext]?.conflateResponses ?: false
