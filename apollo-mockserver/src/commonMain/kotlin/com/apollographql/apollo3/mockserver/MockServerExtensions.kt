@file:JvmName("-MockServers")

package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.ByteString.Companion.encodeUtf8
import kotlin.jvm.JvmName

@ApolloExperimental
fun MockServer.enqueue(string: String, delayMs: Long = 0) {
  val byteString = string.encodeUtf8()
  enqueue(MockResponse(
      statusCode = 200,
      headers = mapOf("Content-Length" to byteString.size.toString()),
      body = byteString,
      delayMillis = delayMs
  ))
}

@ApolloExperimental
fun MockServer.enqueueMultipart(
    parts: List<String>,
    statusCode: Int = 200,
    partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    boundary: String = "-",
) {
  enqueue(createMultipartMixedChunkedResponse(
      parts = parts,
      statusCode = statusCode,
      partsContentType = partsContentType,
      headers = headers,
      responseDelayMillis = responseDelayMillis,
      chunksDelayMillis = chunksDelayMillis,
      boundary = boundary
  ))
}

@ApolloExperimental
fun createMultipartMixedChunkedResponse(
    parts: List<String>,
    statusCode: Int = 200,
    partsContentType: String = "application/json; charset=utf-8",
    headers: Map<String, String> = emptyMap(),
    responseDelayMillis: Long = 0,
    chunksDelayMillis: Long = 0,
    boundary: String = "-",
): MockResponse {
  return MockResponse(
      statusCode = statusCode,
      delayMillis = responseDelayMillis,
      headers = headers + mapOf("Content-Type" to """multipart/mixed; boundary="$boundary""""),
      chunks = parts.mapIndexed { index, part ->
        val startBoundary = if (index == 0) "--$boundary\r\n" else ""
        val contentLengthHeader = "Content-Length: ${part.length}"
        val contentTypeHeader = "Content-Type: $partsContentType"
        val endBoundary = if (index == parts.lastIndex) "--$boundary--" else "--$boundary"
        MockChunk(
            body = startBoundary +
                "$contentLengthHeader\r\n" +
                "$contentTypeHeader\r\n" +
                "\r\n" +
                "$part\r\n" +
                "$endBoundary\r\n",
            delayMillis = chunksDelayMillis
        )
      }
  )
}
