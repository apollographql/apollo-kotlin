package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloParseException
import com.apollographql.apollo.http.OkHttpExecutionContext
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import okhttp3.Response
import java.io.Closeable
import java.util.concurrent.Executor

/**
 * ApolloParseInterceptor is a concrete [ApolloInterceptor] responsible for inflating the http responses into
 * models. To get the http responses, it hands over the control to the next interceptor in the chain and proceeds to
 * then parse the returned response.
 */
class ApolloParseInterceptor(private val httpCache: HttpCache?,
                             private val customScalarAdapters: CustomScalarAdapters,
                             private val logger: ApolloLogger) : ApolloInterceptor {
  @Volatile
  var disposed = false
  override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                              dispatcher: Executor, callBack: CallBack) {
    if (disposed) return
    chain.proceedAsync(request, dispatcher, object : CallBack {
      override fun onResponse(response: InterceptorResponse) {
        try {
          if (disposed) return
          val result = parse(request.operation as Operation<Operation.Data>, response.httpResponse.get())
          callBack.onResponse(result)
          callBack.onCompleted()
        } catch (e: ApolloException) {
          onFailure(e)
        }
      }

      override fun onFailure(e: ApolloException) {
        if (disposed) return
        callBack.onFailure(e)
      }

      override fun onCompleted() {
        // call onCompleted in onResponse in case of error
      }

      override fun onFetch(sourceType: FetchSourceType) {
        callBack.onFetch(sourceType)
      }
    })
  }

  override fun dispose() {
    disposed = true
  }

  @Throws(ApolloHttpException::class, ApolloParseException::class)
  fun parse(operation: Operation<Operation.Data>, httpResponse: Response): InterceptorResponse {
    val cacheKey = httpResponse.request().header(HttpCache.CACHE_KEY_HEADER)
    return if (httpResponse.isSuccessful) {
      try {
        val httpExecutionContext = OkHttpExecutionContext(httpResponse)
        var parsedResponse = operation.parse(httpResponse.body()!!.source(), customScalarAdapters)

        parsedResponse = parsedResponse
            .toBuilder()
            .fromCache(httpResponse.cacheResponse() != null)
            .executionContext(parsedResponse.executionContext.plus(httpExecutionContext))
            .build()
        if (parsedResponse.hasErrors() && httpCache != null) {
          httpCache.removeQuietly(cacheKey!!)
        }

        InterceptorResponse(httpResponse, parsedResponse)
      } catch (rethrown: Exception) {
        logger.e(rethrown, "Failed to parse network response for operation: %s", operation.name().name())
        closeQuietly(httpResponse)
        httpCache?.removeQuietly(cacheKey!!)
        throw ApolloParseException("Failed to parse http response", rethrown)
      }
    } else {
      logger.e("Failed to parse network response: %s", httpResponse)
      throw ApolloHttpException(httpResponse)
    }
  }

  companion object {
    private fun closeQuietly(closeable: Closeable?) {
      if (closeable != null) {
        try {
          closeable.close()
        } catch (ignored: Exception) {
        }
      }
    }
  }
}