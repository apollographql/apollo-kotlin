package com.apollographql.apollo.network.websocket.internal

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.exception.SubscriptionConnectionException
import com.apollographql.apollo.network.websocket.CLOSE_GOING_AWAY
import com.apollographql.apollo.network.websocket.ClientMessage
import com.apollographql.apollo.network.websocket.CompleteServerMessage
import com.apollographql.apollo.network.websocket.ConnectionAckServerMessage
import com.apollographql.apollo.network.websocket.ConnectionErrorServerMessage
import com.apollographql.apollo.network.websocket.ConnectionKeepAliveServerMessage
import com.apollographql.apollo.network.websocket.DataClientMessage
import com.apollographql.apollo.network.websocket.OperationErrorServerMessage
import com.apollographql.apollo.network.websocket.ParseErrorServerMessage
import com.apollographql.apollo.network.websocket.PingServerMessage
import com.apollographql.apollo.network.websocket.PongServerMessage
import com.apollographql.apollo.network.websocket.ResponseServerMessage
import com.apollographql.apollo.network.websocket.TextClientMessage
import com.apollographql.apollo.network.websocket.WebSocket
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import com.apollographql.apollo.network.websocket.WsProtocol
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * A [SubscribableWebSocket] is the link between the lower level [WebSocket] and GraphQL.
 *
 * A [SubscribableWebSocket] has its own [CoroutineScope] used for ping/pong messages as well as allowing the underlying [WsProtocol] to
 * suspend to retrieve a token in init or start
 *
 * [startOperation] starts a new operation and calls [OperationListener] when the server sends messages.
 *
 */
internal class SubscribableWebSocket(
    webSocketEngine: WebSocketEngine,
    serverUrl: String,
    httpHeaders: List<HttpHeader>,
    private val wsProtocol: WsProtocol,
    private val pingInterval: Duration?,
    private val connectionAcknowledgeTimeout: Duration,
    private val idleTimeout: Duration,
) : WebSocketListener {

  private var lock = reentrantLock()
  private val scope = CoroutineScope(Dispatchers.Default)

  private var idleTimeoutJob: Job? = null
  private var ackTimeoutJob: Job? = null
  private var state: SocketState = SocketState.AwaitOpen
  private var shutdownCause: ApolloException? = null
  private var activeListeners = mutableMapOf<String, OperationListener>()
  private var pending = mutableListOf<ApolloRequest<*>>()

  private var webSocket: WebSocket

  init {
    val headers = if (httpHeaders.any { it.name.lowercase() == "sec-websocket-protocol" }) {
      httpHeaders
    } else {
      httpHeaders + HttpHeader("Sec-WebSocket-Protocol", wsProtocol.name)
    }
    webSocket = webSocketEngine.newWebSocket(serverUrl, headers, this)
  }

  /**
   * @param [markActive] pass true when collecting sockets so that a socket
   * that is about to expire does not expire before the next startOperation()
   */
  fun isShutdown(markActive: Boolean): Boolean = lock.withLock {
    (state == SocketState.ShutDown).also {
      if (!it && markActive) {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
      }
    }
  }

  private fun restartIdleTimeout() {
    idleTimeoutJob?.cancel()
    idleTimeoutJob = scope.launch {
      delay(idleTimeout)

      /*
       * Note: the exception here should never be surfaced upstream as by
       * definition, a websocket is idle only if it has no listeners.
       */
      shutdown(ApolloNetworkException("WebSocket is idle"), CLOSE_GOING_AWAY, "Idle")
    }
  }

  /**
   * Shuts this [SubscribableWebSocket] down. Calls [OperationListener.onTransportError] for every active operation.
   * No more calls to listeners are made.
   *
   * This function does not use [webSocket] and is safe to use from callbacks.
   */
  private fun shutdownInternal(cause: ApolloException) {
    val listeners = mutableListOf<OperationListener>()

    lock.withLock {
      if (state == SocketState.ShutDown) {
        return
      }
      state = SocketState.ShutDown

      scope.cancel()
      shutdownCause = cause

      listeners.addAll(activeListeners.values)
      activeListeners.clear()
    }

    listeners.forEach {
      it.onTransportError(cause)
    }
  }

  /**
   * Shuts this [SubscribableWebSocket] down. Calls [OperationListener.onTransportError] for every active operation.
   * No more calls to listeners are made.
   *
   * Doesn't wait for the peer close. The resources are released asynchronously.
   *
   * Must not be called from [onError] as [onError] is called from a different thread and [webSocket] might not be
   * initialized when this happens.
   */
  fun shutdown(cause: ApolloException, code: Int, reason: String) {
    shutdownInternal(cause)
    webSocket.close(code, reason)
  }

  override fun onOpen() {
    lock.withLock {
      when (state) {
        SocketState.AwaitOpen -> {
          scope.launch {
            webSocket.send(wsProtocol.connectionInit())
          }
          ackTimeoutJob = scope.launch {
            delay(connectionAcknowledgeTimeout)
            shutdown(ApolloNetworkException("Timeout while waiting for connection_ack"), CLOSE_GOING_AWAY, "Timeout while waiting for connection_ack")
          }
          state = SocketState.AwaitAck
        }

        else -> {
          // spurious "open" event
        }
      }
    }
  }

  override fun onMessage(text: String) {
    when (val message = wsProtocol.parseServerMessage(text)) {
      ConnectionAckServerMessage -> {
        ackTimeoutJob?.cancel()
        ackTimeoutJob = null

        lock.withLock {
          if (state != SocketState.AwaitAck) {
            // spurious connection_ack
            return
          }
          state = SocketState.Connected

          if (pingInterval != null) {
            scope.launch {
              while (true) {
                delay(pingInterval)
                wsProtocol.ping()?.let { webSocket.send(it) }
              }
            }
          }

          scope.launch {
            pending.forEach {
              webSocket.send(wsProtocol.operationStart(it))
            }
          }
        }
      }

      is ConnectionErrorServerMessage -> {
        shutdown(SubscriptionConnectionException(message.payload), CLOSE_GOING_AWAY, "Connection error")
      }

      is ResponseServerMessage -> {
        lock.withLock { activeListeners.get(message.id) }?.onResponse(message.response)
      }

      is CompleteServerMessage -> {
        lock.withLock { activeListeners.get(message.id) }?.onComplete()
      }

      is OperationErrorServerMessage -> {
        lock.withLock { activeListeners.get(message.id) }?.onError(message.payload)
      }

      is ParseErrorServerMessage -> {
        // This is an unknown or malformed message
        // It's not 100% clear what we should do here. Should we terminate the operation?
        println("Cannot parse message: '${message.errorMessage}'")
      }

      PingServerMessage -> {
        wsProtocol.pong()?.let {
          webSocket.send(it)
        }
      }

      PongServerMessage -> {
        // nothing to do
      }

      ConnectionKeepAliveServerMessage -> {
        // nothing to do
      }
    }
  }

  override fun onMessage(data: ByteArray) {
    onMessage(data.decodeToString())
  }

  override fun onError(cause: ApolloException) {
    shutdownInternal(cause)
  }

  override fun onClosed(code: Int?, reason: String?) {
    shutdownInternal(ApolloWebSocketClosedException(code ?: CLOSE_GOING_AWAY, reason))
  }

  fun <D : Operation.Data> startOperation(request: ApolloRequest<D>, listener: OperationListener) {
    var cause: ApolloException? = null
    lock.withLock {
      idleTimeoutJob?.cancel()
      idleTimeoutJob = null

      when (state) {
        SocketState.AwaitOpen, SocketState.AwaitAck -> {
          activeListeners.put(request.requestUuid.toString(), listener)
          pending.add(request)
        }

        SocketState.Connected -> {
          activeListeners.put(request.requestUuid.toString(), listener)
          scope.launch { webSocket.send(wsProtocol.operationStart(request)) }
        }

        SocketState.ShutDown -> {
          /**
           * This is very unlikely albeit possible if the websocket errors between the time it
           * is constructed and the time [startOperation] is called
           */
          cause = DefaultApolloException("Apollo: the WebSocket is shut down", shutdownCause)
        }
      }
    }

    if (cause != null) {
      listener.onTransportError(cause!!)
    }
  }

  fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>) {
    val id = request.requestUuid.toString()
    lock.withLock {
      if (activeListeners.containsKey(id)) {
        activeListeners.remove(id)

        scope.launch { webSocket.send(wsProtocol.operationStop(request)) }

        if (activeListeners.isEmpty()) {
          restartIdleTimeout()
        }
      }
    }
  }
}

private enum class SocketState {
  AwaitOpen,
  AwaitAck,
  Connected,
  ShutDown
}

private fun WebSocket.send(clientMessage: ClientMessage) {
  when (clientMessage) {
    is TextClientMessage -> send(clientMessage.text)
    is DataClientMessage -> send(clientMessage.data)
  }
}