@file:Suppress("DEPRECATION")

package com.apollographql.apollo.mockserver.test

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.mockserver.MockServer
import com.apollographql.apollo.mockserver.enqueueMultipart
import com.apollographql.apollo.mockserver.enqueueStrings
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.testing.internal.runTest
import okhttp3.MultipartReader
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipartTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private fun tearDown() {
    mockServer.close()
  }

  /**
   * Check that the multipart encoded by MockServer can be decoded by OkHttp's MultipartReader.
   */
  @Test
  fun receiveAndDecodeParts() = runTest(before = { setUp() }, after = { tearDown() }) {
    val part0 = """{"data":{"song":{"firstVerse":"Now I know my ABC's."}},"hasNext":true}"""
    val part1 = """{"data":{"secondVerse":"Next time won't you sing with me?"},"path":["song"],"hasNext":false}"""
    val boundary = "-"
    mockServer.enqueueMultipart(boundary = boundary, partsContentType = "application/json; charset=utf-8")
        .enqueueStrings(listOf(part0, part1))

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
