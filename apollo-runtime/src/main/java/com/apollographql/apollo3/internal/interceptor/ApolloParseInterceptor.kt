package com.apollographql.apollo3.internal.interceptor

import com.apollographql.apollo3.CacheInfo
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.parseResponseBody
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.http.OkHttpExecutionContext
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import okhttp3.Headers
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
        var parsedResponse = operation.parseResponseBody(httpResponse.body()!!.source(), customScalarAdapters)

        parsedResponse = parsedResponse.copy(
            executionContext = parsedResponse.executionContext.plus(httpExecutionContext) + CacheInfo(httpResponse.cacheResponse() != null)
        )

        if (parsedResponse.hasErrors() && httpCache != null) {
          httpCache.removeQuietly(cacheKey!!)
        }

        InterceptorResponse(httpResponse, parsedResponse)
      } catch (rethrown: Exception) {
        logger.e(rethrown, "Failed to parse network response for operation: %s", operation.name())
        closeQuietly(httpResponse)
        httpCache?.removeQuietly(cacheKey!!)
        throw ApolloParseException("Failed to parse http response", rethrown)
      }
    } else {
      logger.e("Failed to parse network response: %s", httpResponse)
      throw ApolloHttpException(
          statusCode = httpResponse.code(),
          headers = httpResponse.headers().toMap(),
          message = httpResponse.body()?.string() ?: "",
          cause = null
      )
    }
  }

  companion object {
    private fun Headers.toMap(): Map<String, String> = this.names().map {
      it to get(it)!!
    }.toMap()

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
