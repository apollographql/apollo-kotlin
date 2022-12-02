package com.apollographql.apollo3.network

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.get
import com.apollographql.apollo3.testing.internal.runTest
import okio.Buffer
import okio.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpEngineTest {
  // "Hello World" gzipped and hex encoded
  val gzipData = """
1f8b 0800 0000 0000 0003 f348 cdc9 c957
08cf 2fca 4901 0056 b117 4a0b 0000 00   
  """.replace(Regex("\\s"), "")

  private fun String.toByteString(): ByteString {
    val buffer = Buffer()
    chunked(2).forEach {
      buffer.writeByte(it.toInt(16))
    }

    return buffer.readByteString()
  }

  @Test
  fun gzipTest() = runTest {
    val mockServer = MockServer()

    try {
      mockServer.enqueue(MockResponse.Builder()
          .addHeader("content-type", "application/text")
          .addHeader("content-encoding", "gzip")
          .body(gzipData.toByteString())
          .build())

      val engine = DefaultHttpEngine()

      val response = engine.get(mockServer.url())
          .execute()

      val result = response.body?.readUtf8()
      assertEquals("Hello World", result)

    } catch (e: Exception) {
      e.printStackTrace()
    }

    mockServer.stop()
  }
}