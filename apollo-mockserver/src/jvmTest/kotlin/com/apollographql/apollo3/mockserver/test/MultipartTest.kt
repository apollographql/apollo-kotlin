package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.internal.runTest
import okhttp3.MultipartReader
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipartTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  /**
   * Check that the multipart encoded by MockServer can be decoded by OkHttp's MultipartReader.
   */
  @Test
  fun receiveAndDecodeParts() = runTest(before = { setUp() }, after = { tearDown() }) {
    val part0 = """{"data":{"song":{"firstVerse":"Now I know my ABC's."}},"hasNext":true}"""
    val part1 = """{"data":{"secondVerse":"Next time won't you sing with me?"},"path":["song"],"hasNext":false}"""
    val boundary = "-"
    mockServer.enqueueMultipart(listOf(part0, part1), boundary = boundary)

    val httpResponse = DefaultHttpEngine().execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())

    val parts = mutableListOf<MultipartReader.Part>()
    val partBodies = mutableListOf<String>()
    // Taken from https://square.github.io/okhttp/4.x/okhttp/okhttp3/-multipart-reader/
    val multipartReader = MultipartReader(httpResponse.body!!, boundary = boundary)
    multipartReader.use {
      while (true) {
        val part = multipartReader.nextPart() ?: break
        parts.add(part)
        partBodies.add(part.body.readUtf8())
      }
    }

    assertEquals("application/json; charset=utf-8", parts[0].headers["Content-Type"])
    assertEquals(part0.length.toString(), parts[0].headers["Content-Length"])
    assertEquals(part0, partBodies[0])

    assertEquals("application/json; charset=utf-8", parts[1].headers["Content-Type"])
    assertEquals(part1.length.toString(), parts[1].headers["Content-Length"])
    assertEquals(part1, partBodies[1])
  }
}
