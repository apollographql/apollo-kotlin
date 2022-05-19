package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.upload.SingleUploadMutation
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.LoggingInterceptor
import com.apollographql.apollo3.testing.runTest
import okio.BufferedSink
import test.HttpInterceptorTest.LoggingInterceptorDoesntConsumeBodyState.uploadRead
import testFixtureToUtf8
import kotlin.native.concurrent.ThreadLocal
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpInterceptorTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun testLoggingInterceptor() = runTest(before = { setUp() }, after = { tearDown() }) {
    val fullLog = StringBuilder()
    val client = ApolloClient.Builder().networkTransport(HttpNetworkTransport.Builder().serverUrl(
        mockServer.url(),
    ).interceptors(listOf(LoggingInterceptor { log ->
      if (log.startsWith("Post http://")) {
        // Rewrite the first line because MockServer's port is different every time
        fullLog.appendLine("Post http://0.0.0.0/")
        // Ignore this header which is different on JVM/Apple/Js
      } else if (log.lowercase() != "connection: close") {
        fullLog.appendLine(log)
      }
    })).build()).build()

    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    client.query(HeroNameQuery()).execute()
    assertEquals("""
      Post http://0.0.0.0/
      X-APOLLO-OPERATION-ID: 7e7c85cbf5ef3af5641552c55965608a4e5d7243f3116a486d21c3a958d34235
      X-APOLLO-OPERATION-NAME: HeroName
      [end of headers]
      {"operationName":"HeroName","variables":{},"query":"query HeroName { hero { name } }"}

      HTTP: 200
      Content-Length: 322
      [end of headers]
      {
        "data": {
          "hero": {
            "__typename": "Droid",
            "name": "R2-D2"
          }
        },
        "extensions": {
          "cost": {
            "requestedQueryCost": 3,
            "actualQueryCost": 3,
            "throttleStatus": {
              "maximumAvailable": 1000,
              "currentlyAvailable": 997,
              "restoreRate": 50
            }
          }
        }
      }


    """.trimIndent().lowercase(), fullLog.toString().lowercase())
  }

  @Test
  fun testLoggingInterceptorOmitRequestBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    val fullLog = StringBuilder()
    val client = ApolloClient.Builder().networkTransport(HttpNetworkTransport.Builder().serverUrl(
        mockServer.url(),
    ).interceptors(listOf(LoggingInterceptor(logRequestBody = false) { log ->
      if (log.startsWith("Post http://")) {
        // Rewrite the first line because MockServer's port is different every time
        fullLog.appendLine("Post http://0.0.0.0/")
        // Ignore this header which is different on JVM/Apple/Js
      } else if (log.lowercase() != "connection: close") {
        fullLog.appendLine(log)
      }
    })).build()).build()

    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    client.query(HeroNameQuery()).execute()
    assertEquals("""
      Post http://0.0.0.0/
      X-APOLLO-OPERATION-ID: 7e7c85cbf5ef3af5641552c55965608a4e5d7243f3116a486d21c3a958d34235
      X-APOLLO-OPERATION-NAME: HeroName
      [end of headers]
      [request body omitted]

      HTTP: 200
      Content-Length: 322
      [end of headers]
      {
        "data": {
          "hero": {
            "__typename": "Droid",
            "name": "R2-D2"
          }
        },
        "extensions": {
          "cost": {
            "requestedQueryCost": 3,
            "actualQueryCost": 3,
            "throttleStatus": {
              "maximumAvailable": 1000,
              "currentlyAvailable": 997,
              "restoreRate": 50
            }
          }
        }
      }


    """.trimIndent().lowercase(), fullLog.toString().lowercase())
  }

  @Test
  fun loggingInterceptorDoesntConsumeBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    // We only test the data that is sent to the server, we don't really mind the response
    mockServer.enqueue("""
      {
        "data": null
      }
    """.trimIndent())

    val content = "Hello, World!"
    val upload: Upload = object : Upload {
      override val contentType = "text/plain"
      override val contentLength = content.length.toLong()
      override val fileName = "hello.txt"


      override fun writeTo(sink: BufferedSink) {
        uploadRead++
        sink.writeUtf8(content)
      }
    }
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).addHttpInterceptor(LoggingInterceptor()).build()
    apolloClient.mutation(SingleUploadMutation(upload)).execute()
    assertEquals(1, uploadRead)
  }

  // Trick to make the above test happy on Apple (which freezes the request but we stay on the main thread so mutating works)
  @ThreadLocal
  object LoggingInterceptorDoesntConsumeBodyState {
    var uploadRead = 0
  }
}
