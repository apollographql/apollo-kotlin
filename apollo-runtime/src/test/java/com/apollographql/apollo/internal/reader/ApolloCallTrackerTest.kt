package com.apollographql.apollo.internal.reader

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.IdleResourceCallback
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer.compose
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.common.truth.Truth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.ByteString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ApolloCallTrackerTest {
  internal class QueryData : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller {
      throw UnsupportedOperationException()
    }
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
    private val EMPTY_QUERY: Query<QueryData, Operation.Variables> = object : Query<QueryData, Operation.Variables> {
      var operationName: OperationName = object : OperationName {
        override fun name(): String {
          return "EmptyQuery"
        }
      }

      override fun queryDocument(): String {
        return ""
      }

      override fun variables(): Operation.Variables {
        return Operation.EMPTY_VARIABLES
      }

      override fun responseFieldMapper(): ResponseFieldMapper<QueryData> {
        return ResponseFieldMapper { throw UnsupportedOperationException() }
      }

      override fun name(): OperationName {
        return operationName
      }

      override fun operationId(): String {
        return ""
      }

      override fun parse(source: BufferedSource): Response<QueryData> {
        throw UnsupportedOperationException()
      }

      override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters): Response<QueryData> {
        throw UnsupportedOperationException()
      }

      override fun parse(byteString: ByteString): Response<QueryData> {
        throw UnsupportedOperationException()
      }

      override fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters): Response<QueryData> {
        throw UnsupportedOperationException()
      }

      override fun composeRequestBody(
          autoPersistQueries: Boolean,
          withQueryDocument: Boolean,
          scalarTypeAdapters: ScalarTypeAdapters
      ): ByteString {
        return compose(this, autoPersistQueries, withQueryDocument, scalarTypeAdapters)
      }

      override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters): ByteString {
        return compose(this, false, true, scalarTypeAdapters)
      }

      override fun composeRequestBody(): ByteString {
        return compose(this, false, true, ScalarTypeAdapters.DEFAULT)
      }
    }
  }
}