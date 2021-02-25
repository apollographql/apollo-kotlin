package com.apollographql.apollo3.http

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ResponseContext
import okhttp3.Response

/**
 * Http GraphQL execution context, provides access to the raw {@link okhttp3.Response} response.
 */
@ApolloExperimental
class OkHttpExecutionContext(
    response: Response
) : ResponseContext(OkHttpExecutionContext) {
  companion object Key : ExecutionContext.Key<OkHttpExecutionContext>

  val response: Response = response.strip()

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
