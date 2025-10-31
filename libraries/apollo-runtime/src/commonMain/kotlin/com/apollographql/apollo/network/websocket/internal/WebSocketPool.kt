package com.apollographql.apollo.network.websocket.internal

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.websocket.CLOSE_GOING_AWAY
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WsProtocol
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import okio.ByteString
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import kotlin.time.Duration

internal class WebSocketPool(
    private val webSocketEngine: WebSocketEngine,
    private val serverUrl: String?,
    private val wsProtocol: WsProtocol,
    private val connectionAcknowledgeTimeout: Duration,
    private val pingInterval: Duration?,
    private val idleTimeout: Duration,
)  {
  private var lock = reentrantLock()
  private val subscribableWebSockets = mutableMapOf<ByteString, SubscribableWebSocket>()

  private fun cleanupLocked(key: ByteString) {
    val iterator = subscribableWebSockets.iterator()
    while(iterator.hasNext()) {
      val entry = iterator.next()
      if(entry.value.isShutdown(entry.key == key)) {
        iterator.remove()
      }
    }
  }

  /**
   * Closes all the connections in the pool but does not close the pool itself
   *
   * @param cause a cause for closing the connections or `null`
   */
  fun closeAllConnections(cause: ApolloException) = lock.withLock {
    closeAllConnectionsLocked(cause)
  }

  /**
   * Closes all the connections in the pool and the pool itself
   */
  fun close() = lock.withLock {
    closeAllConnectionsLocked(ApolloNetworkException("WebSocketNetworkTransport was closed"))
    webSocketEngine.close()
  }

  private fun closeAllConnectionsLocked(cause: ApolloException) {
    val iterator = subscribableWebSockets.iterator()
    while(iterator.hasNext()) {
      val entry = iterator.next()

      entry.value.shutdown(cause, CLOSE_GOING_AWAY, "Client requested closing the connection")
      iterator.remove()
    }
  }

  private fun key(url: String, httpHeaders: List<HttpHeader>): ByteString {
    val hashingSink = HashingSink.sha256(blackholeSink())
    hashingSink.buffer().apply {
      writeUtf8(url)
      writeByte(0)
      httpHeaders.forEach {
        writeUtf8("${it.name}: ${it.value}")
        writeByte(0)
      }
      flush()
    }

    return hashingSink.hash
  }

  fun acquire(apolloRequest: ApolloRequest<*>): SubscribableWebSocket {
    return lock.withLock {

      val url = apolloRequest.url ?: serverUrl ?: error("ApolloRequest.url is missing for request '${apolloRequest.operation.name()}', did you call ApolloClient.Builder.webSocketServerUrl(url)?")
      val httpHeaders = apolloRequest.httpHeaders.orEmpty()
      val key = key(url, httpHeaders)
      cleanupLocked(key)

      var webSocket = subscribableWebSockets.get(key)
      if (webSocket == null) {
        webSocket = SubscribableWebSocket(
            webSocketEngine = webSocketEngine,
            serverUrl = url,
            httpHeaders = httpHeaders,
            wsProtocol = wsProtocol,
            pingInterval = pingInterval,
            connectionAcknowledgeTimeout = connectionAcknowledgeTimeout,
            idleTimeout = idleTimeout
        )
        subscribableWebSockets.put(key, webSocket)
      }
      webSocket
    }
  }
}