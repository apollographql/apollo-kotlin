package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Upload
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.upload.SingleUploadMutation
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.http.LoggingInterceptor
import com.apollographql.apollo.network.http.LoggingInterceptor.Level
import com.apollographql.apollo.testing.internal.runTest
import okio.BufferedSink
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggingInterceptorTest {
  private lateinit var mockServer: MockServer
  private lateinit var logger: Logger

  private fun setUp() {
    mockServer = MockServer()
    logger = Logger()
  }

  private fun tearDown() {
    mockServer.close()
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
      val actual = fullLog.toString().lowercase().trim()
      if (expected == "") {
        assertEquals("", actual)
        return
      }

      /**
       * This is more involved than expected because the response headers order is not preserved on JS.
       * The order of HTTP headers is not important unless there are multiple headers with the same name.
       * It would be a nice property to keep the HTTP headers order albeit it is currently not the case.
       *
       * See https://youtrack.jetbrains.com/issue/KTOR-6582/
       */
      expected
          .lowercase()
          .toStream()
          .assertEquals(actual.toStream())
    }

    private fun String.toStream(): Stream {
      val responseIndex = indexOf("http: ")
      check(responseIndex > 0)

      val request = substring(0, responseIndex)
      val responseLines = substring(responseIndex).split("\n")
      val endOfHeaders = responseLines.indexOfFirst { it == "[end of headers]" }
      return if (endOfHeaders > 0) {
        Stream(request, responseLines.subList(1, endOfHeaders), responseLines.subList(0, 1) + responseLines.subList(endOfHeaders, responseLines.size))
      } else {
        Stream(request, emptyList(), responseLines)
      }
    }

    private class Stream(val request: String, val responseHeaders: List<String>, val otherResponseLines: List<String>) {
      fun assertEquals(other: Stream) {
        assertEquals(request, other.request)
        assertEquals(responseHeaders.sorted(), other.responseHeaders.sorted())
        assertEquals(otherResponseLines, other.otherResponseLines)
      }
    }
  }


  @Test
  fun levelNone() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.NONE, log = logger::log))
        .build()
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("")
  }

  @Test
  fun levelBasic() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.BASIC, log = logger::log))
        .build()
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/

      HTTP: 200
    """.trimIndent())
  }

  @Test
  fun levelHeaders() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.HEADERS, log = logger::log))
        .build()
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/
      accept: multipart/mixed;deferspec=20220824, application/graphql-response+json, application/json
      [end of headers]

      HTTP: 200
      Content-Type: text/plain
      Content-Length: 322
      [end of headers]
    """.trimIndent())
  }

  @Test
  fun levelBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.BODY, log = logger::log))
        .build()
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))
    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/
      accept: multipart/mixed;deferspec=20220824, application/graphql-response+json, application/json
      [end of headers]
      {"operationName":"HeroName","variables":{},"query":"query HeroName { hero { name } }"}

      HTTP: 200
      Content-Type: text/plain
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
    """.trimIndent())
  }

  @Test
  fun levelBodySingleLineResponse() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(LoggingInterceptor(level = Level.BODY, log = logger::log))
        .build()
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json").replace("\n", ""))

    client.query(HeroNameQuery()).execute()
    logger.assertLog("""
      Post http://0.0.0.0/
      accept: multipart/mixed;deferspec=20220824, application/graphql-response+json, application/json
      [end of headers]
      {"operationName":"HeroName","variables":{},"query":"query HeroName { hero { name } }"}

      HTTP: 200
      Content-Type: text/plain
      Content-Length: 303
      [end of headers]
      {  "data": {    "hero": {      "__typename": "Droid",      "name": "R2-D2"    }  },  "extensions": {    "cost": {      "requestedQueryCost": 3,      "actualQueryCost": 3,      "throttleStatus": {        "maximumAvailable": 1000,        "currentlyAvailable": 997,        "restoreRate": 50      }    }  }}
    """.trimIndent())
  }

  @Test
  fun dontConsumeBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    var uploadRead = 0
    // We only test the data that is sent to the server, we don't really mind the response
    mockServer.enqueueString("""
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
