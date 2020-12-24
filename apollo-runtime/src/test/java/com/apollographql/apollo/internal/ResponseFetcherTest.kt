package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.cache.http.HttpCachePolicy.FetchStrategy
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer.compose
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.google.common.truth.Truth
import okhttp3.OkHttpClient
import okio.BufferedSource
import okio.ByteString
import org.junit.Test

class ResponseFetcherTest {

  private val emptyQuery = object : Query<Operation.Data, Operation.Variables> {
    var operationName: OperationName = object : OperationName {
      override fun name(): String {
        return "emptyQuery"
      }
    }

    override fun queryDocument(): String {
      return ""
    }

    override fun variables(): Operation.Variables {
      return Operation.EMPTY_VARIABLES
    }

    override fun responseFieldMapper(): ResponseFieldMapper<Operation.Data> {
      return ResponseFieldMapper {
        object : Operation.Data {
          override fun marshaller(): ResponseFieldMarshaller {
            throw UnsupportedOperationException()
          }
        }
      }
    }
    
    override fun name(): OperationName {
      return operationName
    }

    override fun operationId(): String {
      return ""
    }

    override fun parse(source: BufferedSource): Response<Operation.Data> {
      throw UnsupportedOperationException()
    }

    override fun parse(source: BufferedSource, customScalarAdapters: CustomScalarAdapters): Response<Operation.Data> {
      throw UnsupportedOperationException()
    }

    override fun parse(byteString: ByteString): Response<Operation.Data> {
      throw UnsupportedOperationException()
    }

    override fun parse(byteString: ByteString, customScalarAdapters: CustomScalarAdapters): Response<Operation.Data> {
      throw UnsupportedOperationException()
    }

    override fun composeRequestBody(
        autoPersistQueries: Boolean,
        withQueryDocument: Boolean,
        customScalarAdapters: CustomScalarAdapters
    ): ByteString {
      return compose(this, autoPersistQueries, withQueryDocument, customScalarAdapters)
    }

    override fun composeRequestBody(customScalarAdapters: CustomScalarAdapters): ByteString {
      return compose(this, false, true, customScalarAdapters)
    }

    override fun composeRequestBody(): ByteString {
      return compose(this, false, true, CustomScalarAdapters.DEFAULT)
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
    Truth.assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(FetchStrategy.CACHE_ONLY)
    Truth.assertThat(realApolloCall.responseFetcher).isEqualTo(ApolloResponseFetchers.NETWORK_ONLY)
  }

  @Test
  fun defaultCacheControl() {
    val apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(OkHttpClient())
        .build()
    val realApolloCall = apolloClient.query<Operation.Data, Operation.Variables>(emptyQuery) as RealApolloCall<*>
    Truth.assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(FetchStrategy.NETWORK_ONLY)
    Truth.assertThat(realApolloCall.responseFetcher).isEqualTo(ApolloResponseFetchers.CACHE_FIRST)
  }
}