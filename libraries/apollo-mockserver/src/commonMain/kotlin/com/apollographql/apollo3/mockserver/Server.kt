package com.apollographql.apollo3.mockserver

import okio.Closeable

internal interface Socket: Closeable {
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

internal interface Server : Closeable {
  /**
   * Starts listening and calls [block] when on incoming connections
   */
  fun listen(block: (socket: Socket) -> Unit)

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

internal class Address(
    val hostname: String,
    val port: Int
)

internal expect fun Server(): Server