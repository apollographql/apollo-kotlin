package com.apollographql.apollo3.internal.reader

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.IdleResourceCallback
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ApolloCallTrackerTest {
  internal class QueryData : Query.Data {
  }

  @get:Rule
  val server = MockWebServer()

  private lateinit var activeCallCounts: MutableList<Int>
  private lateinit var apolloClient: ApolloClient

  @Before
  @Throws(Exception::class)
  fun setUp() {
    activeCallCounts = mutableListOf()
    val interceptor = Interceptor { chain ->
      activeCallCounts.add(apolloClient.activeCallsCount())
      chain.proceed(chain.request())
    }
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build()
  }

  @Test
  @Throws(InterruptedException::class)
  fun testRunningCallsCountWhenSyncPrefetchCallIsMade() {
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
    Rx2Apollo
        .from(apolloClient.prefetch(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
    Truth.assertThat(activeCallCounts).isEqualTo(listOf(1))
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
  }

  @Test
  @Throws(InterruptedException::class)
  fun testRunningCallsCountWhenAsyncPrefetchCallIsMade() {
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
    server.enqueue(createMockResponse())
    Rx2Apollo
        .from(apolloClient.prefetch(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
    Truth.assertThat(activeCallCounts).isEqualTo(listOf(1))
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
  }

  @Test
  @Throws(InterruptedException::class)
  fun testRunningCallsCountWhenAsyncApolloCallIsMade() {
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
    server.enqueue(createMockResponse())
    Rx2Apollo
        .from(apolloClient.query(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
    Truth.assertThat(activeCallCounts).isEqualTo(listOf(1))
    Truth.assertThat(apolloClient.activeCallsCount()).isEqualTo(0)
  }

  @Test
  @Throws(InterruptedException::class, TimeoutException::class)
  fun testIdleCallBackIsInvokedWhenApolloClientBecomesIdle() {
    server.enqueue(createMockResponse())
    val idle = AtomicBoolean()
    val idleResourceCallback = IdleResourceCallback { idle.set(true) }
    apolloClient.idleCallback(idleResourceCallback)
    Truth.assertThat(idle.get()).isFalse()
    Rx2Apollo
        .from(apolloClient.query(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
    Truth.assertThat(idle.get()).isTrue()
  }

  private fun createMockResponse(): MockResponse {
    return MockResponse().setResponseCode(200).setBody(StringBuilder()
        .append("{")
        .append("  \"errors\": [")
        .append("    {")
        .append("      \"message\": \"Cannot query field \\\"names\\\" on type \\\"Species\\\".\",")
        .append("      \"locations\": [")
        .append("        {")
        .append("          \"line\": 3,")
        .append("          \"column\": 5")
        .append("        }")
        .append("      ]")
        .append("    }")
        .append("  ]")
        .append("}")
        .toString())
  }

  companion object {
    private const val SERVER_URL = "http://localhost:1234"
    private const val TIMEOUT_SECONDS = 2
    private val EMPTY_QUERY: Query<QueryData> = object : Query<QueryData> {
      var operationName: String  = "EmptyQuery"

      override fun queryDocument(): String {
        return ""
      }

      override fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache) {
        writer.beginObject()
        writer.endObject()
      }

      override fun adapter(responseAdapterCache: ResponseAdapterCache) = throw UnsupportedOperationException()

      override fun name(): String {
        return operationName
      }

      override fun operationId(): String {
        return ""
      }

      override fun responseFields(): List<ResponseField.FieldSet> {
        return emptyList()
      }
    }
  }
}
