package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.upload.SingleUploadMutation
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.LoggingInterceptor
import com.apollographql.apollo3.network.http.LoggingInterceptor.Level
import com.apollographql.apollo3.testing.internal.runTest
import okio.BufferedSink
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggingInterceptorTest {
  private lateinit var mockServer: MockServer
  private lateinit var logger: Logger

  private suspend fun setUp() {
    mockServer = MockServer()
    logger = Logger()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  private class Logger {
    val fullLog = StringBuilder()

    fun log(log: String) {
      if (log.startsWith("Post http://")) {
        // Rewrite the first line because MockServer's port is different every time
        fullLog.appendLine("Post http://0.0.0.0/")
        // Ignore this header which is different on JVM/Apple/Js
      } else if (log.lowercase() != "connection: close") {
        fullLog.appendLine(log)
      }
    }

    fun assertLog(expected: String) {
      assertEquals(expected.trimIndent().lowercase(), fullLog.toString().lowercase().trim())
    }
  }

  @Test
  fun levelNone() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.NONE, log = logger::log))
        .build()
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("")
  }

  @Test
  fun levelBasic() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.BASIC, log = logger::log))
        .build()
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/

      HTTP: 200
    """)
  }

  @Test
  fun levelHeaders() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.HEADERS, log = logger::log))
        .build()
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/
      X-APOLLO-OPERATION-ID: 7e7c85cbf5ef3af5641552c55965608a4e5d7243f3116a486d21c3a958d34235
      X-APOLLO-OPERATION-NAME: HeroName
      accept: multipart/mixed; deferspec=20220824, application/json
      [end of headers]

      HTTP: 200
      Content-Length: 322
      [end of headers]
    """)
  }

  @Test
  fun levelBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.BODY, log = logger::log))
        .build()
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/
      X-APOLLO-OPERATION-ID: 7e7c85cbf5ef3af5641552c55965608a4e5d7243f3116a486d21c3a958d34235
      X-APOLLO-OPERATION-NAME: HeroName
      accept: multipart/mixed; deferspec=20220824, application/json
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
    """)
  }

  @Test
  fun dontConsumeBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    var uploadRead = 0
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
}
