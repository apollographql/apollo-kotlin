package com.apollographql.apollo

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer.compose
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.json.JsonEncodingException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloParseException
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.ByteString
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
        .from(apolloClient.query<Operation.Data, Operation.Variables>(emptyQuery))
        .doOnError { throwable ->
          errorRef.set(throwable)
          errorResponse.set((throwable as ApolloHttpException).rawResponse()!!.body()!!.string())
        }
        .test()
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertError(ApolloHttpException::class.java)
    val e = errorRef.get() as ApolloHttpException
    assertThat(e.code()).isEqualTo(401)
    assertThat(e.message()).isEqualTo("Client Error")
    assertThat(errorResponse.get()).isEqualTo("Unauthorized request!")
    assertThat(e.message).isEqualTo("HTTP 401 Client Error")
  }

  @Test
  @Throws(Exception::class)
  fun httpExceptionPrefetch() {
    server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized request!"))
    Rx2Apollo
        .from(apolloClient.prefetch<Operation.Data, Operation.Variables>(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(ApolloHttpException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testTimeoutException() {
    Rx2Apollo
        .from(apolloClient.query<Operation.Data, Operation.Variables>(emptyQuery))
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
        .from(apolloClient.prefetch<Operation.Data, Operation.Variables>(emptyQuery))
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
        .from(apolloClient.query<Operation.Data, Operation.Variables>(emptyQuery))
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