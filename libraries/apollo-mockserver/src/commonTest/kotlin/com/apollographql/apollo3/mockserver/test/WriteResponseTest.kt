package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.asChunked
import com.apollographql.apollo3.mockserver.createMultipartMixedChunkedResponse
import com.apollographql.apollo3.mockserver.writeResponse
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.flowOf
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class WriteResponseTest {
  @Test
  fun writeResponse() = runTest {
    val mockResponse = MockResponse.Builder()
        .statusCode(404)
        .addHeader("X-Custom-Header", "Custom-Value")
        .body("I will not buy this record, it is scratched.")
        .build()

    val buffer = Buffer()
    @Suppress("DEPRECATION")
    writeResponse(buffer, mockResponse, "1.1")
    assertEquals(
        "1.1 404\r\n" +
            "X-Custom-Header: Custom-Value\r\n" +
            "Content-Length: 44\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "I will not buy this record, it is scratched.",
        buffer.readUtf8()
    )
  }

  @Test
  fun writeChunkedResponse() = runTest {
    val mockResponse = MockResponse.Builder()
        .statusCode(404)
        .headers(mapOf("X-Custom-Header" to "Custom-Value", "Transfer-Encoding" to "chunked"))
        .body(flowOf("I will not buy this record, ".encodeUtf8(), "it is scratched.".encodeUtf8()).asChunked())
        .build()

    val buffer = Buffer()
    @Suppress("DEPRECATION")
    writeResponse(buffer, mockResponse, "1.1")
    assertEquals(
        "1.1 404\r\n" +
            "X-Custom-Header: Custom-Value\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "1c\r\n" +
            "I will not buy this record, \r\n" +
            "10\r\n" +
            "it is scratched.\r\n" +
            "0\r\n" +
            "\r\n",
        buffer.readUtf8()
    )
  }

  @Test
  fun writeMultipartChunkedResponse() = runTest {
    val mockResponse = createMultipartMixedChunkedResponse(listOf(
        """{"data":{"song":{"firstVerse":"Now I know my ABC's."}},"hasNext":true}""",
        """{"data":{"secondVerse":"Next time won't you sing with me?"},"path":["song"],"hasNext":false}"""
    ))

    val buffer = Buffer()
    @Suppress("DEPRECATION")
    writeResponse(buffer, mockResponse, "1.1")
    assertEquals(
        listOf(
            "1.1 200",
            """Content-Type: multipart/mixed; boundary="-"""",
            "Transfer-Encoding: chunked",
            "Connection: close",
            "",
            "97",
            "---",
            "Content-Length: 70",
            "Content-Type: application/json; charset=utf-8",
            "",
            """{"data":{"song":{"firstVerse":"Now I know my ABC's."}},"hasNext":true}""",
            "---",
            "",
            "aa",
            "Content-Length: 92",
            "Content-Type: application/json; charset=utf-8",
            "",
            """{"data":{"secondVerse":"Next time won't you sing with me?"},"path":["song"],"hasNext":false}""",
            "-----",
            "",
            "0",
            "",
        ).joinToString("\r\n", postfix = "\r\n"),
        buffer.readUtf8()
    )
  }

}
