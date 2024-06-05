package com.apollographql.apollo3.network.websocket.internal

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.exception.SubscriptionConnectionException
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.network.websocket.CLOSE_GOING_AWAY
import com.apollographql.apollo3.network.websocket.CLOSE_NORMAL
import com.apollographql.apollo3.network.websocket.ClientMessage
import com.apollographql.apollo3.network.websocket.CompleteServerMessage
import com.apollographql.apollo3.network.websocket.ConnectionAckServerMessage
import com.apollographql.apollo3.network.websocket.ConnectionErrorServerMessage
import com.apollographql.apollo3.network.websocket.ConnectionKeepAliveServerMessage
import com.apollographql.apollo3.network.websocket.DataClientMessage
import com.apollographql.apollo3.network.websocket.OperationErrorServerMessage
import com.apollographql.apollo3.network.websocket.ParseErrorServerMessage
import com.apollographql.apollo3.network.websocket.PingServerMessage
import com.apollographql.apollo3.network.websocket.PongServerMessage
import com.apollographql.apollo3.network.websocket.ResponseServerMessage
import com.apollographql.apollo3.network.websocket.TextClientMessage
import com.apollographql.apollo3.network.websocket.WebSocket
import com.apollographql.apollo3.network.websocket.WebSocketEngine
import com.apollographql.apollo3.network.websocket.WebSocketListener
import com.apollographql.apollo3.network.websocket.WsProtocol
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private val dispatcher: CoroutineDispatcher,
    private val wsProtocol: WsProtocol,
    private val pingIntervalMillis: Long,
    private val connectionAcknowledgeTimeoutMillis: Long,
) : WebSocketListener {

  private var lock = reentrantLock()
  private val scope = CoroutineScope(dispatcher)

  private var ackTimeoutJob: Job? = null
  private var state: SocketState = SocketState.AwaitOpen
  private var shutdownCause: ApolloException? = null
  private var activeListeners = mutableMapOf<String, OperationListener>()
  private var pending = mutableListOf<ApolloRequest<*>>()
  private var _lastActiveMillis: Long = 0

  private var webSocket: WebSocket
  init {
    val headers = if (httpHeaders.any { it.name.lowercase() == "sec-websocket-protocol" }) {
      httpHeaders
    } else {
      httpHeaders + HttpHeader("Sec-WebSocket-Protocol", wsProtocol.name)
    }
    webSocket = webSocketEngine.newWebSocket(serverUrl, headers, this)
  }

  val lastActiveMillis: Long
    get() = lock.withLock {
      _lastActiveMillis
    }
  val shutdown: Boolean
    get() = lock.withLock {
      state == SocketState.ShutDown
    }

  fun shutdown(cause: ApolloException?, code: Int?, reason: String?) {
    val listeners = mutableListOf<OperationListener>()

    lock.withLock {
      if (state == SocketState.ShutDown) {
        return
      }
      scope.cancel()

      state = SocketState.ShutDown
      shutdownCause = cause
      listeners.addAll(activeListeners.values)
      activeListeners.clear()
    }

    if (code != null && reason != null) {
      webSocket.close(code, reason)
    }

    listeners.forEach {
      if (cause == null) {
        it.onComplete()
      } else {
        it.onTransportError(cause)
      }
    }
  }

  override fun onOpen() {
    lock.withLock {
      when (state) {
        SocketState.AwaitOpen -> {
          scope.launch(dispatcher) {
            webSocket.send(wsProtocol.connectionInit())
          }
          ackTimeoutJob = scope.launch(dispatcher) {
            delay(connectionAcknowledgeTimeoutMillis)
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

          if (pingIntervalMillis > 0) {
            scope.launch {
              while (true) {
                delay(pingIntervalMillis)
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
    shutdown(cause, null, null)
  }

  override fun onClosed(code: Int?, reason: String?) {
    shutdown(ApolloWebSocketClosedException(code ?: CLOSE_NORMAL, reason), null, null)
  }

  fun <D : Operation.Data> startOperation(request: ApolloRequest<D>, listener: OperationListener) {
    var cause: ApolloException? = null
    lock.withLock {
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
    val removed = lock.withLock {
      var ret = false
      if (activeListeners.containsKey(id)) {
        activeListeners.remove(id)
        ret = true
      }

      if (activeListeners.isEmpty()) {
        _lastActiveMillis = currentTimeMillis()
      }
      ret
    }

    if (!removed) {
      return
    }

    scope.launch { webSocket.send(wsProtocol.operationStop(request)) }
  }

  fun markActive() = lock.withLock {
    _lastActiveMillis = 0
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