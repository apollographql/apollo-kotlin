package com.apollographql.apollo3.mockserver

import okio.Closeable

internal interface Socket: Closeable {
  /**
   * Suspends until data is received and returned the data
   *
   * @throws [okio.IOException] if there is an error reading data
   */
  suspend fun receive(): ByteArray

  /**
   * Writes [data] with no flow control.
   * If data cannot be transmitted fast enough, it will be buffered.
   * There is no guarantee that the data is actually transmitted or processed by the remote side.
   */
  fun write(data: ByteArray): Boolean

  override fun close()
}

internal interface SocketServer : Closeable {
  fun start(block: (socket: Socket) -> Unit)
  suspend fun address(): Address
}

internal class Address(
    val hostname: String,
    val port: Int
)
internal expect fun SocketServer(acceptDelayMillis: Int = 0): SocketServer