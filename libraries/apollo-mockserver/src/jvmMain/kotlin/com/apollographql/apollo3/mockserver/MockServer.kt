package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import okio.source
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

@Suppress("BlockingMethodInNonBlockingContext")
actual class MockServer actual constructor(
    override val mockServerHandler: MockServerHandler,
) : MockServerInterface {
  private val serverSocket = ServerSocket(0)
  private val mockRequests = mutableListOf<MockRequest>()

  private val dispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
  private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

  init {
    coroutineScope.launch {
      while (true) {
        val clientSocket = try {
          serverSocket.accept()
        } catch (_: Exception) {
          // An exception here means the server socket has been closed (stop() was called)
          break
        }
        launch { handleClient(clientSocket) }
      }
    }
  }

  private suspend fun handleClient(clientSocket: Socket) {
    try {
      val clientSource = clientSocket.getInputStream().source().buffer()
      val clientSink = clientSocket.getOutputStream().sink().buffer()

      val mockRequest = readRequest(clientSource)!!
      synchronized(mockRequests) {
        mockRequests += mockRequest
      }

      val mockResponse = mockServerHandler.handle(mockRequest)
      delay(mockResponse.delayMillis)
      @Suppress("DEPRECATION")
      writeResponse(clientSink, mockResponse, mockRequest.version)
    } catch (e: CancellationException) {
      // This is expected when the MockServer is closed
      throw e
    } catch (e: Exception) {
      println("Apollo: error in MockServer while handling client - $e")
      e.printStackTrace()
    } finally {
      runCatching { clientSocket.close() }
    }
  }

  override fun enqueue(mockResponse: MockResponse) {
    (mockServerHandler as? QueueMockServerHandler)?.enqueue(mockResponse)
        ?: error("Apollo: cannot call MockServer.enqueue() with a custom handler")
  }

  override fun takeRequest(): MockRequest {
    return synchronized(mockRequests) { mockRequests.removeFirst() }
  }

  override suspend fun url(): String {
    return "http://${serverSocket.inetAddress.hostAddress}:${serverSocket.localPort}/"
  }

  override suspend fun stop() {
    runCatching { serverSocket.close() }
    coroutineScope.cancel()
    dispatcher.close()
  }
}
