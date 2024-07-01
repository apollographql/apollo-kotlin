package com.apollographql.apollo

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.MutableExecutionOptions
import com.apollographql.apollo.api.Operation

/**
 * When [conflateResponses] is true, the fetch policy interceptors emit a single response and ignores
 * the first cache or network error if a successful response is ultimately fetched.
 * If no successful response can be emitted, a [com.apollographql.apollo.exception.ApolloCompositeException]
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
@ApolloInternal
fun <T> MutableExecutionOptions<T>.conflateFetchPolicyInterceptorResponses(conflateResponses: Boolean) = addExecutionContext(
    ConflateResponsesContext(conflateResponses)
)

internal class ConflateResponsesContext(val conflateResponses: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ConflateResponsesContext>
}

@ApolloInternal
val <D : Operation.Data> ApolloRequest<D>.conflateFetchPolicyInterceptorResponses
  get() = executionContext[ConflateResponsesContext]?.conflateResponses ?: false
