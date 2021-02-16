package com.apollographql.apollo3.http

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ExecutionContext
import okhttp3.Response

/**
 * Http GraphQL execution context, provides access to the raw {@link okhttp3.Response} response.
 */
@ApolloExperimental
class OkHttpExecutionContext(
    response: Response
) : ExecutionContext.Element {
  val response: Response = response.strip()

  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<OkHttpExecutionContext> {
    @JvmField
    val KEY: Key = Key
  }

  private fun Response.strip(): Response {
    val builder = newBuilder()

    if (body() != null) {
      builder.body(null)
    }

    val cacheResponse = cacheResponse()
    if (cacheResponse != null) {
      builder.cacheResponse(cacheResponse.strip())
    }

    val networkResponse = networkResponse()
    if (networkResponse != null) {
      builder.networkResponse(networkResponse.strip())
    }

    return builder.build()
  }
}
