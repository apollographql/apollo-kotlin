package com.apollographql.apollo.internal.batch

import com.apollographql.apollo.Utils
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor
import com.apollographql.apollo.request.RequestHeaders
import com.google.common.base.Predicate
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.Timeout
import org.junit.Test

class BatchHttpRequestTest {

  @Test
  fun testCombiningOperationsInRequestBody() {
    val planetsQuery = AllPlanetsQuery()
    val episodeQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val customHeaderName = "appHeader"
    val customHeaderValue = "appHeaderValue"
    val queryList = listOf(
        QueryToBatch(
            ApolloInterceptor.InterceptorRequest.builder(planetsQuery)
                .requestHeaders(RequestHeaders.builder().addHeader(customHeaderName, customHeaderValue).build())
                .build(),
            NoOpCallback()
        ),
        QueryToBatch(ApolloInterceptor.InterceptorRequest.builder(episodeQuery).build(), NoOpCallback())
    )
    val predicate = Predicate<Request> { request ->
      assertThat(request).isNotNull()
      assertThat(request!!.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.ACCEPT_TYPE)
      assertThat(request.header(customHeaderName)).isEqualTo(customHeaderValue)
      assertRequestBody(request)
      true
    }
    BatchHttpCallImpl(
        queryList,
        HttpUrl.get("https://google.com"),
        AssertHttpCallFactory(predicate),
        ScalarTypeAdapters(emptyMap())
    ).execute()
  }

  private fun assertRequestBody(request: Request?) {
    assertThat(request!!.body()!!.contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE)
    val bodyBuffer = Buffer()
    try {
      request.body()!!.writeTo(bodyBuffer)
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
    Utils.checkTestFixture(bodyBuffer.readUtf8(), "BatchHttpRequestTest/expectedBatchRequestBody.json")
  }

  private class AssertHttpCallFactory constructor(val predicate: Predicate<Request>) : Call.Factory {
    override fun newCall(request: Request): Call {
      if (!predicate.apply(request)) {
        Assert.fail("Assertion failed")
      }
      return NoOpCall()
    }
  }

  private class NoOpCall : Call {
    override fun request(): Request {
      throw UnsupportedOperationException()
    }

    override fun execute(): Response {
      throw UnsupportedOperationException()
    }

    override fun enqueue(responseCallback: Callback) {}
    override fun cancel() {}
    override fun isExecuted(): Boolean {
      return false
    }

    override fun isCanceled(): Boolean {
      return false
    }

    override fun clone(): Call {
      throw UnsupportedOperationException()
    }

    override fun timeout(): Timeout {
      throw UnsupportedOperationException()
    }
  }

  private class NoOpCallback: ApolloInterceptor.CallBack {
    override fun onResponse(response: ApolloInterceptor.InterceptorResponse) {
    }

    override fun onFetch(sourceType: ApolloInterceptor.FetchSourceType?) {
    }

    override fun onFailure(e: ApolloException) {
    }

    override fun onCompleted() {
    }
  }
}