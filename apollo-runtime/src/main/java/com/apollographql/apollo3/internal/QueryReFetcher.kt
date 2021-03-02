package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorFactory
import com.apollographql.apollo3.internal.RealApolloCall.Companion.builder
import okhttp3.Call
import okhttp3.HttpUrl
import java.util.ArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class QueryReFetcher(builder: Builder) {
  val logger: ApolloLogger?
  private val calls: MutableList<RealApolloCall<Operation.Data>>
  private val queryWatchers: List<String>
  private val callTracker: ApolloCallTracker?
  private val executed = AtomicBoolean()
  var onCompleteCallback: OnCompleteCallback? = null

  fun refetch() {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
    refetchQueryWatchers()
    refetchQueries()
  }

  fun cancel() {
    for (call in calls) {
      call.cancel()
    }
  }

  private fun refetchQueryWatchers() {
    try {
      for (operationName in queryWatchers) {
        for (queryWatcher in callTracker!!.activeQueryWatchers(operationName)) {
          queryWatcher.refetch()
        }
      }
    } catch (e: Exception) {
      logger!!.e(e, "Failed to re-fetch query watcher")
    }
  }

  private fun refetchQueries() {
    val completeCallback = onCompleteCallback
    val callsLeft = AtomicInteger(calls.size)
    for (call in calls) {
      call.enqueue(object : ApolloCall.Callback<Operation.Data>() {
        override fun onResponse(response: ApolloResponse<Operation.Data>) {
          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete()
          }
        }

        override fun onFailure(e: ApolloException) {
          logger?.e(e, "Failed to fetch query: %s", call.operation)
          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete()
          }
        }
      })
    }
  }

  class Builder {
    var queries: List<Query<*>> = emptyList()
    var queryWatchers: List<String> = emptyList()
    var serverUrl: HttpUrl? = null
    var httpCallFactory: Call.Factory? = null
    var responseAdapterCache: ResponseAdapterCache? = null
    var apolloStore: ApolloStore? = null
    var dispatcher: Executor? = null
    var logger: ApolloLogger? = null
    var applicationInterceptors: List<ApolloInterceptor>? = null
    var applicationInterceptorFactories: List<ApolloInterceptorFactory>? = null
    var autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory? = null
    var callTracker: ApolloCallTracker? = null
    fun queries(queries: List<Query<*>>?): Builder {
      this.queries = queries ?: emptyList()
      return this
    }

    fun queryWatchers(queryWatchers: List<String>?): Builder {
      this.queryWatchers = queryWatchers ?: emptyList()
      return this
    }

    fun serverUrl(serverUrl: HttpUrl?): Builder {
      this.serverUrl = serverUrl
      return this
    }

    fun httpCallFactory(httpCallFactory: Call.Factory?): Builder {
      this.httpCallFactory = httpCallFactory
      return this
    }

    fun scalarTypeAdapters(responseAdapterCache: ResponseAdapterCache?): Builder {
      this.responseAdapterCache = responseAdapterCache
      return this
    }

    fun apolloStore(apolloStore: ApolloStore?): Builder {
      this.apolloStore = apolloStore
      return this
    }

    fun dispatcher(dispatcher: Executor?): Builder {
      this.dispatcher = dispatcher
      return this
    }

    fun logger(logger: ApolloLogger?): Builder {
      this.logger = logger
      return this
    }

    fun applicationInterceptors(applicationInterceptors: List<ApolloInterceptor>?): Builder {
      this.applicationInterceptors = applicationInterceptors
      return this
    }

    fun applicationInterceptorFactories(applicationInterceptorFactories: List<ApolloInterceptorFactory>?): Builder {
      this.applicationInterceptorFactories = applicationInterceptorFactories
      return this
    }

    fun autoPersistedOperationsInterceptorFactory(interceptorFactories: ApolloInterceptorFactory?): Builder {
      autoPersistedOperationsInterceptorFactory = interceptorFactories
      return this
    }

    fun callTracker(callTracker: ApolloCallTracker?): Builder {
      this.callTracker = callTracker
      return this
    }

    fun build(): QueryReFetcher {
      return QueryReFetcher(this)
    }
  }

  interface OnCompleteCallback {
    fun onFetchComplete()
  }

  companion object {
    fun builder(): Builder {
      return Builder()
    }
  }

  init {
    logger = builder.logger
    calls = ArrayList(builder.queries.size)
    for (query in builder.queries) {
      calls.add(builder<Operation.Data>()
          .operation(query)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .scalarTypeAdapters(builder.responseAdapterCache)
          .apolloStore(builder.apolloStore)
          .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
          .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
          .cacheHeaders(CacheHeaders.NONE)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .applicationInterceptorFactories(builder.applicationInterceptorFactories)
          .autoPersistedOperationsInterceptorFactory(builder.autoPersistedOperationsInterceptorFactory)
          .tracker(builder.callTracker)
          .dispatcher(builder.dispatcher)
          .build())
    }
    queryWatchers = builder.queryWatchers
    callTracker = builder.callTracker
  }
}
