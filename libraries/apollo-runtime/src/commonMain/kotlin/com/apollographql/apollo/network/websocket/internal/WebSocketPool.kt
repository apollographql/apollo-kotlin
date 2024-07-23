package com.apollographql.apollo.network.websocket.internal

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.websocket.CLOSE_GOING_AWAY
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WsProtocol
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.time.Duration

internal class WebSocketPool(
    private val webSocketEngine: WebSocketEngine,
    private val serverUrl: String,
    private val wsProtocol: WsProtocol,
    private val connectionAcknowledgeTimeout: Duration,
    private val pingInterval: Duration?,
    private val idleTimeout: Duration,
)  {
  private var lock = reentrantLock()
  private val subscribableWebSockets = mutableMapOf<List<HttpHeader>, SubscribableWebSocket>()

  private fun cleanupLocked(key: List<HttpHeader>) {
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

  fun acquire(httpHeaders: List<HttpHeader>): SubscribableWebSocket {
    return lock.withLock {

      cleanupLocked(httpHeaders)

      var webSocket = subscribableWebSockets.get(httpHeaders)
      if (webSocket == null) {
        webSocket = SubscribableWebSocket(
            webSocketEngine = webSocketEngine,
            serverUrl = serverUrl,
            httpHeaders = httpHeaders,
            wsProtocol = wsProtocol,
            pingInterval = pingInterval,
            connectionAcknowledgeTimeout = connectionAcknowledgeTimeout,
            idleTimeout = idleTimeout
        )
        subscribableWebSockets.put(httpHeaders, webSocket)
      }
      webSocket
    }
  }
}