package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ExecutionContext

@ApolloExperimental
sealed class HttpExecutionContext {

  data class Request(val headers: Map<String, String>) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<*> = Key

    companion object Key : ExecutionContext.Key<Request>
  }

  data class Response(
      val statusCode: Int,
      val headers: Map<String, String>
  ) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<*> = Key

    companion object Key : ExecutionContext.Key<Response>
  }
}
