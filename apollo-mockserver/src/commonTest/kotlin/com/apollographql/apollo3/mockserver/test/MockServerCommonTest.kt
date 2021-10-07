package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.mockserver.readRequest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MockServerCommonTest {
  @Test
  fun readGetRequest() {
    val request = """
      GET / HTTP/2
      Host: github.com
      User-Agent: curl/7.64.1
      Accept: */*
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "\r\n\r\n")

    val recordedRequest = readRequest(Buffer().apply { writeUtf8(request) })
    assertNotNull(recordedRequest)
    assertEquals("GET", recordedRequest.method)
    assertEquals("/", recordedRequest.path)
    assertEquals("HTTP/2", recordedRequest.version)
    assertEquals(mapOf(
        "Host" to "github.com",
        "User-Agent" to "curl/7.64.1",
        "Accept" to "*/*"
    ), recordedRequest.headers)
    assertEquals(0, recordedRequest.body.size)
  }

  @Test
  fun readPostRequest() {
    val request = """
      POST / HTTP/2
      Content-Length: 11
      
      Hello world
    """.trimIndent()
        .split("\n")
        .joinToString(separator = "\r\n", postfix = "")

    val recordedRequest = readRequest(Buffer().apply { writeUtf8(request) })
    assertNotNull(recordedRequest)
    assertEquals("POST", recordedRequest.method)
    assertEquals("Hello world", recordedRequest.body.utf8())
  }

}