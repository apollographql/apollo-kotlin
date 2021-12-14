package com.apollographql.apollo3.mockserver

import Buffer
import net.AddressInfo
import okio.ByteString.Companion.toByteString
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class MockServer : MockServerInterface {
  private val responseQueue = mutableListOf<MockResponse>()
  private val requests = mutableListOf<MockRecordedRequest>()

  private val server = http.createServer { req, res ->
    val requestBody = StringBuilder()
    req.on("data") { chunk ->
      when (chunk) {
        is String -> requestBody.append(chunk)
        is Buffer -> requestBody.append(chunk.toString("utf8"))
        else -> println("WTF")
      }
    }
    req.on("end") { _ ->
      requests.add(
          MockRecordedRequest(
              req.method,
              req.url,
              req.httpVersion,
              req.rawHeaders.toList().zipWithNext().toMap(),
              requestBody.toString().encodeToByteArray().toByteString()
          )
      )
    }

    val mockResponse = responseQueue.removeFirst()
    res.statusCode = mockResponse.statusCode
    mockResponse.headers.forEach {
      res.setHeader(it.key, it.value)
    }
    res.end(mockResponse.body.utf8())
  }.listen()

  override suspend fun url() = suspendCoroutine<String> { cont ->
    server.on("listening") { _ ->
      cont.resume("http://localhost:${server.address().unsafeCast<AddressInfo>().port}")
    }
  }

  override fun enqueue(mockResponse: MockResponse) {
    responseQueue.add(mockResponse)
  }

  override fun takeRequest(): MockRecordedRequest {
    return requests.removeFirst()
  }

  override suspend fun stop() = suspendCoroutine<Unit> { cont ->
    server.close {
      cont.resume(Unit)
    }
  }
}
