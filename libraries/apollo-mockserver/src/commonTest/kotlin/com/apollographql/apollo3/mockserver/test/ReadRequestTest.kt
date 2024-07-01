package com.apollographql.apollo.mockserver.test

import com.apollographql.apollo.mockserver.MockRequest
import com.apollographql.apollo.mockserver.Reader
import com.apollographql.apollo.mockserver.readRequest
import com.apollographql.apollo.testing.internal.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private fun String.toReader(): Reader {
  val buffer = Buffer().writeUtf8(this)

  return object : Reader {
    override val buffer: Buffer
      get() = buffer

    override suspend fun fillBuffer() {
      error("Buffer is exhausted")
    }
  }
}

class ReadRequestTest {
  @Test
  fun readGetRequest() = runTest {
    val request = """
      GET / HTTP/2
      Host: github.com
      User-Agent: curl/7.64.1
      Accept: */*
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "\r\n\r\n")

    val recordedRequest = readRequest(request.toReader())

    assertNotNull(recordedRequest)
    assertEquals("GET", recordedRequest.method)
    assertEquals("/", recordedRequest.path)
    assertEquals("HTTP/2", recordedRequest.version)
    assertEquals(mapOf(
        "Host" to "github.com",
        "User-Agent" to "curl/7.64.1",
        "Accept" to "*/*"
    ), recordedRequest.headers)
    assertEquals(0, (recordedRequest as MockRequest).body.size)
  }

  @Test
  fun readPostRequest() = runTest {
    val request = """
      POST / HTTP/2
      Content-Length: 11
      
      Hello world
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "")

    val recordedRequest = readRequest(request.toReader())

    assertNotNull(recordedRequest)
    assertEquals("POST", recordedRequest.method)
    assertEquals("Hello world", (recordedRequest as MockRequest).body.utf8())
  }
}
