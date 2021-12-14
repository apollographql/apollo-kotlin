package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import kotlin.jvm.JvmOverloads

@Deprecated("Use ApolloClient.Builder.batching instead")
class BatchingHttpEngine @JvmOverloads constructor(
    val delegate: HttpEngine = DefaultHttpEngine(),
    batchIntervalMillis: Long = 10,
    maxBatchSize: Int = 10,
    exposeErrorBody: Boolean = false,
) : HttpEngine {

  private val batchingHttpInterceptor = BatchingHttpInterceptor(batchIntervalMillis, maxBatchSize, exposeErrorBody)
  private val engineInterceptor = object : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      return delegate.execute(request)
    }
  }
  private val interceptorChain = DefaultHttpInterceptorChain(
      interceptors = listOf(engineInterceptor),
      index = 0,
  )

  override suspend fun execute(request: HttpRequest): HttpResponse {
    return batchingHttpInterceptor.intercept(request, interceptorChain)
  }

  override fun dispose() {
    delegate.dispose()
  }
}
