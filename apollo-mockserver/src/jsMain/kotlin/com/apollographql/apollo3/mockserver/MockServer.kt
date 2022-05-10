package com.apollographql.apollo3.mockserver

import Buffer
import http.ServerResponse
import net.AddressInfo
import okio.ByteString.Companion.toByteString
import setTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

actual class MockServer actual constructor(override val mockServerHandler: MockServerHandler) : MockServerInterface {

  private val requests = mutableListOf<MockRequest>()

  private var url: String? = null

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
      val request = MockRequest(
          req.method,
          req.url,
          req.httpVersion,
          req.rawHeaders.toList().zipWithNext().toMap(),
          requestBody.toString().encodeToByteArray().toByteString()
      )
      requests.add(request)

      val mockResponse = try {
        mockServerHandler.handle(request)
      } catch (e: Exception) {
        throw Exception("MockServerHandler.handle() threw an exception: ${e.message}", e)
      }

      schedule(mockResponse.delayMillis) {
        res.statusCode = mockResponse.statusCode
        mockResponse.headers.forEach {
          res.setHeader(it.key, it.value)
        }
        if (mockResponse.chunks.isNotEmpty()) {
          sendChunksWithDelays(res, mockResponse.chunks)
        } else {
          res.end(mockResponse.body.utf8())
        }
      }
    }
  }.listen()

  private fun sendChunksWithDelays(res: ServerResponse, chunks: List<MockChunk>) {
    val promises = mutableListOf<Promise<Unit>>()
    var delayMillis = 0L
    for (chunk in chunks) {
      delayMillis += chunk.delayMillis
      promises += schedule(delayMillis) {
        res.write(chunk.body.utf8())
      }
    }
    Promise.all(promises.toTypedArray()).then {
      res.end()
    }
  }

  private fun schedule(delayMillis: Long, block: () -> Unit) = Promise<Unit> { resolve, _ ->
    setTimeout({
      block()
      resolve(Unit)
    }, delayMillis)
  }


  override suspend fun url() = url ?: suspendCoroutine { cont ->
    url = "http://localhost:${server.address().unsafeCast<AddressInfo>().port}/"
    server.on("listening") { _ ->
      cont.resume(url!!)
    }
  }

  override fun enqueue(mockResponse: MockResponse) {
    (mockServerHandler as? QueueMockServerHandler)?.enqueue(mockResponse)
        ?: error("Apollo: cannot call MockServer.enqueue() with a custom handler")
  }

  override fun takeRequest(): MockRequest {
    return requests.removeFirst()
  }

  override suspend fun stop() = suspendCoroutine<Unit> { cont ->
    server.close {
      cont.resume(Unit)
    }
  }
}
