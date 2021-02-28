package com.apollographql.apollo3.test.espresso

import androidx.test.espresso.IdlingResource.ResourceCallback
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.internal.OperationRequestBodyComposer.compose
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.ByteString
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.apollographql.apollo3.test.espresso.ApolloIdlingResource
import org.junit.Before

class ApolloIdlingResourceTest {
  @get:Rule
  val server = MockWebServer()

  private lateinit var apolloClient: ApolloClient

  @Before
  fun setup() {
    apolloClient = ApolloClient.builder()
        .okHttpClient(OkHttpClient())
        .serverUrl(server.url("/"))
        .build()
  }

  @Test
  fun checkValidIdlingResourceNameIsRegistered() {
    val idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient)
    Truth.assertThat(idlingResource.getName()).isEqualTo(IDLING_RESOURCE_NAME)
  }

  @Test
  @Throws(InterruptedException::class)
  fun checkIsIdleNow_whenCallIsQueued() {
    server.enqueue(mockResponse().setHeadersDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS))
    val latch = CountDownLatch(1)
    val executorService = Executors.newFixedThreadPool(1)
    apolloClient = ApolloClient.builder()
        .okHttpClient(OkHttpClient())
        .dispatcher(executorService)
        .serverUrl(server.url("/"))
        .build()
    val idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient)
    Truth.assertThat(idlingResource.isIdleNow()).isTrue()
    apolloClient.query(EMPTY_QUERY).enqueue(object : ApolloCall.Callback<Query.Data>() {
      override fun onResponse(response: ApolloResponse<Query.Data>) {
        latch.countDown()
      }

      override fun onFailure(e: ApolloException) {
        throw AssertionError("This callback can't be called.")
      }
    })
    Truth.assertThat(idlingResource.isIdleNow()).isFalse()
    latch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS)
    executorService.shutdown()
    executorService.awaitTermination(TIME_OUT_SECONDS, TimeUnit.SECONDS)
    Thread.sleep(100)
    Truth.assertThat(idlingResource.isIdleNow()).isTrue()
  }

  @Test
  @Throws(InterruptedException::class)
  fun checkIsIdleNow_whenCallIsWatched() {
    server.enqueue(mockResponse())
    val latch = CountDownLatch(1)
    val executorService = Executors.newFixedThreadPool(1)

    val idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient)
    Truth.assertThat(idlingResource.isIdleNow()).isTrue()
    apolloClient.query(EMPTY_QUERY).watcher().enqueueAndWatch(object : ApolloCall.Callback<Query.Data>() {
      override fun onResponse(response: ApolloResponse<Query.Data>) {
        latch.countDown()
      }

      override fun onFailure(e: ApolloException) {
        throw AssertionError("This callback can't be called.")
      }
    })
    Truth.assertThat(idlingResource.isIdleNow()).isFalse()
    latch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS)
    executorService.shutdown()
    executorService.awaitTermination(TIME_OUT_SECONDS, TimeUnit.SECONDS)
    Thread.sleep(100)
    Truth.assertThat(idlingResource.isIdleNow()).isTrue()
  }

  @Test
  @Throws(ApolloException::class)
  fun checkIdlingResourceTransition_whenCallIsQueued() {
    server.enqueue(mockResponse())
    apolloClient = ApolloClient.builder()
        .okHttpClient(OkHttpClient())
        .dispatcher { command -> command.run() }
        .serverUrl(server.url("/"))
        .build()
    val counter = AtomicInteger(1)
    val idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient)
    idlingResource.registerIdleTransitionCallback(ResourceCallback { counter.decrementAndGet() })
    Truth.assertThat(counter.get()).isEqualTo(1)
    Rx2Apollo.from(apolloClient.query(EMPTY_QUERY)).test().awaitTerminalEvent()
    Truth.assertThat(counter.get()).isEqualTo(0)
  }

  private fun mockResponse(): MockResponse {
    return MockResponse().setResponseCode(200).setBody("{" +
        "  \"errors\": [" +
        "    {" +
        "      \"message\": \"Cannot query field \\\"names\\\" on type \\\"Species\\\".\"," +
        "      \"locations\": [" +
        "        {" +
        "          \"line\": 3," +
        "          \"column\": 5" +
        "        }" +
        "      ]" +
        "    }" +
        "  ]" +
        "}")
  }

  companion object {
    private const val TIME_OUT_SECONDS: Long = 3
    private const val IDLING_RESOURCE_NAME = "apolloIdlingResource"
    private val EMPTY_QUERY: Query<Query.Data> = object : Query<Query.Data> {
      override fun queryDocument(): String {
        return ""
      }

      override fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache) {
        writer.beginObject()
        writer.endObject()
      }

      override fun adapter(responseAdapterCache: ResponseAdapterCache): ResponseAdapter<Query.Data> {
        return object: ResponseAdapter<Query.Data> {
          override fun fromResponse(reader: JsonReader): Query.Data {
            while (reader.selectName(emptyList()) != -1) {
              // consume the json stream
            }
            return object: Query.Data {}
          }

          override fun toResponse(writer: JsonWriter, value: Query.Data) {
            TODO("Not yet implemented")
          }
        }
      }

      override fun name(): String = "EmptyQuery"

      override fun operationId(): String {
        return ""
      }
      override fun responseFields(): List<ResponseField.FieldSet> {
        return emptyList()
      }
    }
  }
}
