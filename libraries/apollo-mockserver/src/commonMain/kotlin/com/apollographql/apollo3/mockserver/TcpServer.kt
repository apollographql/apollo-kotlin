package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import okio.Closeable

@ApolloExperimental
interface TcpSocket: Closeable {
  /**
   * Suspend until data is received and returns any available data
   *
   * @throws [okio.IOException] if there is an error reading data
   */
  suspend fun receive(): ByteArray

  /**
   * Schedules data to be sent.
   *
   * Data is buffered unbounded.
   *
   * There is no guarantee that the data is actually transmitted or processed by the remote side.
   */
  fun send(data: ByteArray)

  /**
   * Closes the socket.
   *
   * Sends TCP FIN packet.
   * Pending or subsequent [receive] calls throw [okio.IOException]
   */
  override fun close()
}

@ApolloExperimental
interface TcpServer : Closeable {
  /**
   * Starts listening and calls [block] when on incoming connections
   */
  fun listen(block: (socket: TcpSocket) -> Unit)

  /**
   * Returns the local address the server is bound to. Only valid after calling [listen]
   */
  suspend fun address(): Address

  /**
   * Closes the server.
   *
   */
  override fun close()
}

@ApolloExperimental
class Address(
    val hostname: String,
    val port: Int
)

@ApolloExperimental
expect fun TcpServer(port: Int): TcpServer