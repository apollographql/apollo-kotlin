package com.apollographql.apollo3

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.json.JsonEncodingException
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ApolloExceptionTest {
  @get:Rule
  val server = MockWebServer()

  private lateinit var apolloClient: ApolloClient

  private val emptyQuery = object : Query<Query.Data> {
    var operationName: String = "emptyQuery"

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


  @Before
  fun setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build())
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun httpException() {
    server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized request!"))
    val errorRef = AtomicReference<Throwable>()
    val errorResponse = AtomicReference<String>()
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .doOnError { throwable ->
          errorRef.set(throwable)
          errorResponse.set((throwable as ApolloHttpException).message)
        }
        .test()
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertError(ApolloHttpException::class.java)
    val e = errorRef.get() as ApolloHttpException
    assertThat(e.statusCode).isEqualTo(401)
    assertThat(errorResponse.get()).isEqualTo("Unauthorized request!")
    assertThat(e.message).isEqualTo("Unauthorized request!")
  }

  @Test
  @Throws(Exception::class)
  fun httpExceptionPrefetch() {
    server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized request!"))
    Rx2Apollo
        .from(apolloClient.prefetch(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(ApolloHttpException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testTimeoutException() {
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds * 2, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError { throwable ->
          val e = throwable as ApolloNetworkException
          assertThat(e.message).isEqualTo("Failed to execute http call")
          assertThat(e.cause?.javaClass).isEqualTo(SocketTimeoutException::class.java)
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun testTimeoutExceptionPrefetch() {
    Rx2Apollo
        .from(apolloClient.prefetch(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds * 2, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError { throwable ->
          val e = throwable as ApolloNetworkException
          assertThat(e.message).isEqualTo("Failed to execute http call")
          assertThat(e.cause?.javaClass).isEqualTo(SocketTimeoutException::class.java)
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun testParseException() {
    server.enqueue(MockResponse().setBody("Noise"))
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError { throwable ->
          val e = throwable as ApolloParseException
          assertThat(e.message).isEqualTo("Failed to parse http response")
          assertThat(e.cause?.javaClass).isEqualTo(JsonEncodingException::class.java)
          true
        }
  }

  companion object {
    private const val timeoutSeconds: Long = 2
  }
}
