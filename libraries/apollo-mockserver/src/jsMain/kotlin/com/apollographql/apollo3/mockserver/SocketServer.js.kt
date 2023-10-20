package com.apollographql.apollo3.mockserver

import Buffer
import kotlinx.coroutines.channels.Channel
import net.AddressInfo
import net.createServer
import okio.IOException
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import net.Socket as NetSocket

internal class NodeSocket(private val netSocket: NetSocket) : Socket {
  private val readQueue = Channel<ByteArray>(Channel.UNLIMITED)
  init {
    netSocket.on("data") { chunk ->
      val bytes = when (chunk) {
        is String -> chunk.encodeToByteArray()
        is Buffer -> chunk.asByteArray()
        else -> error("Unexpected chunk type: ${chunk::class}")
      }
      readQueue.trySend(bytes)
    }

    netSocket.on("close") { _ ->
      readQueue.close(IOException("The socket was closed"))
    }
  }

  override suspend fun receive(): ByteArray {
    return readQueue.receive()
  }

  // XXX: flow control
  override fun write(data: ByteArray): Boolean {
    return netSocket.write(data.asUint8Array())
  }

  override fun close() {
    readQueue.close()
    netSocket.destroy()
  }
}

internal class NodeSocketServer : SocketServer {
  private var server: net.Server? = null
  private var address: Address? = null


  override fun start(block: (socket: Socket) -> Unit) {
    server = createServer { netSocket ->
      block(NodeSocket(netSocket))
    }

    server!!.listen()
  }

  override suspend fun address(): Address {
    check(server != null) {
      "You need to call start() before calling port()"
    }

    return address ?: suspendCoroutine { cont ->
      server!!.on("listening") { _ ->
        val address = server!!.address().unsafeCast<AddressInfo>()

        this.address = Address(address.address, address.port.toInt())
        cont.resume(this.address!!)
      }
    }
  }

  override fun close() {
    check(server != null) {
      "server is not started"
    }
    server!!.close()
  }
}

private fun Uint8Array.asByteArray(): ByteArray {
  return Int8Array(buffer, byteOffset, length).unsafeCast<ByteArray>()
}

private fun ByteArray.asUint8Array(): Uint8Array {
  return Uint8Array(length = size).apply {
    for (i in indices) {
      set(i, get(i))
    }
  }
}

internal actual fun SocketServer(acceptDelayMillis: Int): SocketServer = NodeSocketServer()