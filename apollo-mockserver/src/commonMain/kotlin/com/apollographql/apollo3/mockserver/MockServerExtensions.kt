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
    waitForMoreChunks: Boolean = false,
): MockResponse {
  return MockResponse(
      statusCode = statusCode,
      delayMillis = responseDelayMillis,
      headers = headers + mapOf("Content-Type" to """multipart/mixed; boundary="$boundary""""),
      chunks = parts.mapIndexed { index, content ->
        createMultipartMixedChunk(
            content = content,
            contentType = partsContentType,
            boundary = boundary,
            isFirst = index == 0,
            isLast = index == parts.lastIndex,
            delayMillis = chunksDelayMillis
        )
      },
      waitForMoreChunks = waitForMoreChunks,
  )
}

@ApolloExperimental
fun createMultipartMixedChunk(
    content: String,
    contentType: String = "application/json; charset=utf-8",
    boundary: String = "-",
    isFirst: Boolean = false,
    isLast: Boolean = false,
    delayMillis: Long = 0,
): MockChunk {
  val startBoundary = if (isFirst) "--$boundary\r\n" else ""
  val contentLengthHeader = "Content-Length: ${content.length}"
  val contentTypeHeader = "Content-Type: $contentType"
  val endBoundary = if (isLast) "--$boundary--" else "--$boundary"
  return MockChunk(
      body = startBoundary +
          "$contentLengthHeader\r\n" +
          "$contentTypeHeader\r\n" +
          "\r\n" +
          "$content\r\n" +
          "$endBoundary\r\n",
      delayMillis = delayMillis
  )
}
