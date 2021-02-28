package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.internal.Optional
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.request.RequestHeaders
import okhttp3.Response
import java.util.UUID
import java.util.concurrent.Executor

/**
 * ApolloInterceptor is responsible for observing and modifying the requests going out and the corresponding responses
 * coming back in. Typical responsibilities include adding or removing headers from the request or response objects,
 * transforming the returned responses from one type to another, etc.
 */
interface ApolloInterceptor {
  /**
   * Intercepts the outgoing request and performs non blocking operations on the request or the response returned by the
   * next set of interceptors in the chain.
   *
   * @param request    outgoing request object.
   * @param chain      the ApolloInterceptorChain object containing the next set of interceptors.
   * @param dispatcher the Executor which dispatches the non blocking operations on the request/response.
   * @param callBack   the Callback which will handle the interceptor's response or failure exception.
   */
  fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                     dispatcher: Executor, callBack: CallBack)

  /**
   * Disposes of the resources which are no longer required.
   *
   *
   * A use case for this method call would be when an [com.apollographql.apollo3.ApolloCall] needs to be
   * cancelled and resources need to be disposed of.
   */
  fun dispose()

  /**
   * Handles the responses returned by [ApolloInterceptor]
   */
  interface CallBack {
    /**
     * Gets called when the interceptor returns a response after successfully performing operations on the
     * request/response. May be called multiple times.
     *
     * @param response The response returned by the interceptor.
     */
    fun onResponse(response: InterceptorResponse)

    /**
     * Called when interceptor starts fetching response from source type
     *
     * @param sourceType type of source been used to fetch response from
     */
    fun onFetch(sourceType: FetchSourceType)

    /**
     * Gets called when an unexpected exception occurs while performing operations on the request or processing the
     * response returned by the next set of interceptors. Will be called at most once.
     */
    fun onFailure(e: ApolloException)

    /**
     * Called after the last call to [.onResponse]. Do not call after [.onFailure].
     */
    fun onCompleted()
  }

  /**
   * Fetch source type
   */
  enum class FetchSourceType {
    /**
     * Response is fetched from the cache (SQLite or memory or both)
     */
    CACHE,

    /**
     * Response is fetched from the network
     */
    NETWORK
  }

  /**
   * InterceptorResponse class represents the response returned by the [ApolloInterceptor].
   */
  class InterceptorResponse @JvmOverloads constructor(httpResponse: Response?, parsedResponse: com.apollographql.apollo3.api.ApolloResponse<*>? = null) {
    val httpResponse: Optional<Response>
    @JvmField
    val parsedResponse: Optional<com.apollographql.apollo3.api.ApolloResponse<*>?>

    init {
      this.httpResponse = Optional.fromNullable(httpResponse)
      this.parsedResponse = Optional.fromNullable(parsedResponse)
    }
  }

  /**
   * Request to be proceed with [ApolloInterceptor]
   */
  class InterceptorRequest internal constructor(val operation: Operation<*>, val cacheHeaders: CacheHeaders, val requestHeaders: RequestHeaders,
                                                val optimisticUpdates: Optional<Operation.Data>, val fetchFromCache: Boolean,
                                                val sendQueryDocument: Boolean, val useHttpGetMethodForQueries: Boolean, val autoPersistQueries: Boolean) {
    val uniqueId = UUID.randomUUID()
    fun toBuilder(): Builder {
      return Builder(operation)
          .cacheHeaders(cacheHeaders)
          .requestHeaders(requestHeaders)
          .fetchFromCache(fetchFromCache)
          .optimisticUpdates(optimisticUpdates.orNull())
          .sendQueryDocument(sendQueryDocument)
          .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
          .autoPersistQueries(autoPersistQueries)
    }

    class Builder internal constructor(operation: Operation<*>) {
      private val operation: Operation<*>
      private var cacheHeaders = CacheHeaders.NONE
      private var requestHeaders = RequestHeaders.NONE
      private var fetchFromCache = false
      private var optimisticUpdates = Optional.absent<Operation.Data>()
      private var sendQueryDocument = true
      private var useHttpGetMethodForQueries = false
      private var autoPersistQueries = false
      fun cacheHeaders(cacheHeaders: CacheHeaders): Builder {
        this.cacheHeaders = cacheHeaders
        return this
      }

      fun requestHeaders(requestHeaders: RequestHeaders): Builder {
        this.requestHeaders = requestHeaders
        return this
      }

      fun fetchFromCache(fetchFromCache: Boolean): Builder {
        this.fetchFromCache = fetchFromCache
        return this
      }

      fun optimisticUpdates(optimisticUpdates: Operation.Data?): Builder {
        this.optimisticUpdates = Optional.fromNullable(optimisticUpdates)
        return this
      }

      fun optimisticUpdates(optimisticUpdates: Optional<Operation.Data>): Builder {
        this.optimisticUpdates = optimisticUpdates
        return this
      }

      fun sendQueryDocument(sendQueryDocument: Boolean): Builder {
        this.sendQueryDocument = sendQueryDocument
        return this
      }

      fun useHttpGetMethodForQueries(useHttpGetMethodForQueries: Boolean): Builder {
        this.useHttpGetMethodForQueries = useHttpGetMethodForQueries
        return this
      }

      fun autoPersistQueries(autoPersistQueries: Boolean): Builder {
        this.autoPersistQueries = autoPersistQueries
        return this
      }

      fun build(): InterceptorRequest {
        return InterceptorRequest(operation, cacheHeaders, requestHeaders, optimisticUpdates,
            fetchFromCache, sendQueryDocument, useHttpGetMethodForQueries, autoPersistQueries)
      }

      init {
        this.operation = operation
      }
    }

    companion object {
      @JvmStatic
      fun builder(operation: Operation<*>): Builder {
        return Builder(operation)
      }
    }
  }
}