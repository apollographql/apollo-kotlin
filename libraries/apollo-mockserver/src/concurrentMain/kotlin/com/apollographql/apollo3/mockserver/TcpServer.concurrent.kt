package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.IOException
import io.ktor.network.sockets.Socket as WrappedSocket

actual fun TcpServer(port: Int): TcpServer = KtorTcpServer(port)

@ApolloExperimental
class KtorTcpServer(port: Int = 0, private val acceptDelayMillis: Int = 0, dispatcher: CoroutineDispatcher = Dispatchers.IO) : TcpServer {
  private val selectorManager = SelectorManager(dispatcher)
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val serverSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", port)


  override fun close() {
    scope.cancel()
    selectorManager.close()
    serverSocket.close()
  }

  override fun listen(block: (socket: TcpSocket) -> Unit) {
    scope.launch {
      while (true) {
        if (acceptDelayMillis > 0) {
          delay(acceptDelayMillis.toLong())
        }
        val socket: WrappedSocket = serverSocket.accept()
        val ktorSocket = KtorTcpSocket(socket)
        block(ktorSocket)

        launch {
          ktorSocket.loop()
        }
      }
    }
  }

  override suspend fun address(): Address {
    return withTimeout(1000) {
      var address: Address
      while(true) {
        try {
          address = (serverSocket.localAddress as InetSocketAddress).let {
            Address(it.hostname, it.port)
          }
          break
        } catch (e: Exception) {
          delay(50)
          continue
        }
      }
      address
    }
  }
}

internal class KtorTcpSocket(private val socket: WrappedSocket) : TcpSocket {
  private val receiveChannel = socket.openReadChannel()
  private val writeChannel = socket.openWriteChannel()

  private val writeQueue = Channel<ByteArray>(Channel.UNLIMITED)
  private val readQueue = Channel<ByteArray>(Channel.UNLIMITED)

  private suspend fun readLoop() {
    val buffer = ByteArray(8192)
    while (true) {
      val ret = receiveChannel.readAvailable(buffer, 0, buffer.size)
      if (ret == -1) {
        readQueue.close(IOException("Error reading socket", receiveChannel.closedCause))
        break
      } else if (ret > 0) {
        readQueue.send(ByteArray(ret) { buffer[it] })
      }
    }
  }

  private suspend fun writeLoop() {
    while (true) {
      val data = writeQueue.receive()
      writeChannel.writeFully(data, 0, data.size)
      writeChannel.flush()
    }
  }

  suspend fun loop() = coroutineScope {
    launch {
      readLoop()
      // Whenever the socket get closed, cancel the writeLoop
      this@coroutineScope.cancel()
    }
    launch {
      writeLoop()
    }
  }

  override fun close() {
    socket.close()
  }

  override suspend fun receive(): ByteArray {
    return readQueue.receive()
  }

  override fun send(data: ByteArray) {
    writeQueue.trySend(data)
  }
}