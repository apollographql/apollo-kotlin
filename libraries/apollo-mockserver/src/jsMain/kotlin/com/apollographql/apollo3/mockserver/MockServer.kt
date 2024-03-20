package com.apollographql.apollo3.mockserver

import Buffer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.AddressInfo
import net.Socket
import net.createServer
import okio.Sink
import okio.Timeout
import okio.buffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class MockServer actual constructor(override val mockServerHandler: MockServerHandler) : MockServerInterface {

  private val requests = mutableListOf<MockRequest>()

  private var url: String? = null

  @OptIn(DelicateCoroutinesApi::class)
  private val server = createServer { socket ->
    val requestBody = okio.Buffer()
    socket.on("data") { chunk ->
      when (chunk) {
        is String -> requestBody.writeUtf8(chunk)
        is Buffer -> requestBody.write(chunk.asByteArray())
        else -> error("Unexpected chunk type: ${chunk::class}")
      }
      val request = readRequest(requestBody)!!
      requests.add(request)

      val mockResponse = try {
        mockServerHandler.handle(request)
      } catch (e: Exception) {
        throw Exception("MockServerHandler.handle() threw an exception: ${e.message}", e)
      }

      GlobalScope.launch {
        delay(mockResponse.delayMillis)
        @Suppress("DEPRECATION")
        writeResponse(SocketSink(socket).buffer(), mockResponse, request.version)
        socket.end()
      }
    }
  }.listen()

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

  private fun Uint8Array.asByteArray(): ByteArray {
    return Int8Array(buffer, byteOffset, length).unsafeCast<ByteArray>()
  }

  private class SocketSink(private val socket: Socket) : Sink {
    override fun write(source: okio.Buffer, byteCount: Long) {
      socket.write(source.toUint8Array(byteCount))
    }

    private fun okio.Buffer.toUint8Array(count: Long): Uint8Array {
      val array = Uint8Array(count.toInt())

      for (i in 0.until(count.toInt())) {
        array.set(i.toInt(), get(i.toLong()))
      }

      skip(count)
      return array
    }
    override fun close() {}
    override fun flush() {}
    override fun timeout() = Timeout.NONE
  }
}
