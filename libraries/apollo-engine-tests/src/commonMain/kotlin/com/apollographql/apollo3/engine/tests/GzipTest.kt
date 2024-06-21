package com.apollographql.apollo3.engine.tests

import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.get
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import okio.Buffer
import okio.ByteString
import kotlin.test.assertEquals

// "Hello World" gzipped and hex encoded
private val gzipData = """
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

suspend fun gzipTest(engine: HttpEngine) {
  val mockServer = MockServer()

  try {
    mockServer.enqueue(MockResponse.Builder()
        .addHeader("content-type", "application/text")
        .addHeader("content-encoding", "gzip")
        .body(gzipData.toByteString())
        .build()
    )

    val response = engine.get(mockServer.url())
        .execute()

    val result = response.body?.readUtf8()
    assertEquals("Hello World", result)

  } catch (e: Exception) {
    e.printStackTrace()
  }

  mockServer.close()
  engine.close()
}
