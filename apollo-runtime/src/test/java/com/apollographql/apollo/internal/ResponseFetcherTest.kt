package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.cache.http.HttpCachePolicy.FetchStrategy
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.google.common.truth.Truth
import okhttp3.OkHttpClient
import org.junit.Test

class ResponseFetcherTest {

  private val emptyQuery = object : Query<Operation.Data> {
    var operationName: String  ="emptyQuery"

    override fun queryDocument(): String {
      return ""
    }

    override fun variables(): Operation.Variables {
      return Operation.EMPTY_VARIABLES
    }

    override fun adapter() = throw UnsupportedOperationException()

    override fun name(): String {
      return operationName
    }

    override fun operationId(): String {
      return ""
    }
  }

  @Test
  fun setDefaultCachePolicy() {
    val apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(OkHttpClient())
        .defaultHttpCachePolicy(HttpCachePolicy.CACHE_ONLY)
        .defaultResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .build()
    val realApolloCall = apolloClient.query(emptyQuery) as RealApolloCall<*>
    Truth.assertThat(realApolloCall.httpCachePolicy!!.fetchStrategy).isEqualTo(FetchStrategy.CACHE_ONLY)
    Truth.assertThat(realApolloCall.responseFetcher).isEqualTo(ApolloResponseFetchers.NETWORK_ONLY)
  }

  @Test
  fun defaultCacheControl() {
    val apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(OkHttpClient())
        .build()
    val realApolloCall = apolloClient.query<Operation.Data>(emptyQuery) as RealApolloCall<*>
    Truth.assertThat(realApolloCall.httpCachePolicy!!.fetchStrategy).isEqualTo(FetchStrategy.NETWORK_ONLY)
    Truth.assertThat(realApolloCall.responseFetcher).isEqualTo(ApolloResponseFetchers.CACHE_FIRST)
  }
}
