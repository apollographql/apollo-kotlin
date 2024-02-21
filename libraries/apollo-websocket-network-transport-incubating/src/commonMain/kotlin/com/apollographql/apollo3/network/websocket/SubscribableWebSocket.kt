package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.exception.SubscriptionOperationException
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [SubscribableWebSocket] is the link between the lower level [WebSocket] and GraphQL
 *
 * [startOperation] starts a new operation and calls [WebSocketOperationListener] when the server sends messages.
 *
 */
internal class SubscribableWebSocket(
    webSocketEngine: WebSocketEngine,
    url: String,
    headers: List<HttpHeader>,
    private val idleTimeoutMillis: Long,
    private val onConnected: () -> Unit,
    private val onDisconnected: (Throwable, listeners: List<WebSocketOperationListener>) -> Unit,
    private val dispatcher: CoroutineDispatcher,
    private val wsProtocol: WsProtocol,
    private val pingIntervalMillis: Long,
    private val connectionAcknowledgeTimeoutMillis: Long,
) : WebSocketListener {

  // webSocket is thread safe, no need to lock
  private var webSocket: WebSocket = webSocketEngine.newWebSocket(url, headers, this@SubscribableWebSocket)
  private val scope = CoroutineScope(dispatcher + SupervisorJob())

  // locked fields, these fields may be accessed from different threads and require locking
  private val lock = reentrantLock()
  private var timeoutJob: Job? = null
  private var state: State = State.Initial
  private var throwable: Throwable? = null
  private var activeListeners = mutableMapOf<String, ActiveOperationListener>()
  private var idleJob: Job? = null
  private var pingJob: Job? = null
  private var pending = mutableListOf<ApolloRequest<*>>()
  // end of locked fields

  init {
    webSocket.connect()
  }

  private fun disconnect(throwable: Throwable) {
    val listeners = lock.withLock {
      pingJob?.cancel()
      pingJob = null
      if (state != State.Disconnected) {
        state = State.Disconnected
        this.throwable = throwable
        activeListeners.values.forEach {
          it.terminated = true
        }
        activeListeners.values.toList()
      } else {
        return
      }
    }

    onDisconnected(throwable, listeners.map { it.operationListener })
  }

  override fun onOpen() {
    lock.withLock {
      when (state) {
        State.Initial -> {
          scope.launch(dispatcher) {
            webSocket.send(wsProtocol.connectionInit())
          }
          timeoutJob = scope.launch(dispatcher) {
            delay(connectionAcknowledgeTimeoutMillis)
            webSocket.close(CLOSE_GOING_AWAY, "Timeout while waiting for connection_ack")
            disconnect(DefaultApolloException("Timeout while waiting for ack"))
          }
          state = State.AwaitAck
        }

        else -> {
          // spurious "open" event
        }
      }
    }
  }

  private fun listenerFor(id: String): WebSocketOperationListener? = lock.withLock {
    activeListeners.get(id)?.let {
      if (it.terminated) {
        null
      } else {
        it.operationListener
      }
    }
  }

  override fun onMessage(text: String) {
    when (val message = wsProtocol.parseServerMessage(text)) {
      ConnectionAckServerMessage -> {
        timeoutJob?.cancel()
        timeoutJob = null

        lock.withLock {
          state = State.Connected

          if (pingIntervalMillis > 0) {
            pingJob = scope.launch {
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

        onConnected()
      }

      is ConnectionErrorServerMessage -> {
        disconnect(ApolloNetworkException("Connection error"))
        webSocket.close(CLOSE_GOING_AWAY, "Connection Error")
      }

      is ResponseServerMessage -> {
        listenerFor(message.id)?.let {
          it.onResponse(message.response)
          if (message.complete) {
            it.onComplete()
          }
        }
      }

      is CompleteServerMessage -> {
        listenerFor(message.id)?.onComplete()
      }

      is OperationErrorServerMessage -> {
        listenerFor(message.id)?.onError(SubscriptionOperationException("Server send an error", message.payload))
      }

      is ParseErrorServerMessage -> {
        // This is an unknown or malformed message
        // It's not 100% clear what we should do here. Should we terminate the operation?
        println("Cannot parse message: '${message.errorMessage}'")
      }

      PingServerMessage -> {
        scope.launch {
          val pong = wsProtocol.pong()
          if (pong != null) {
            webSocket.send(pong)
          }
        }
      }

      PongServerMessage -> {
        // nothing to do
      }

      ConnectionKeepAliveServerMessage ->  {
        // nothing to do?
      }

      is GeneralErrorServerMessage -> {
        disconnect(message.exception)
      }
    }
  }

  override fun onMessage(data: ByteArray) {
    onMessage(data.decodeToString())
  }

  override fun onError(throwable: Throwable) {
    disconnect(throwable)
  }

  override fun onClosed(code: Int?, reason: String?) {
    disconnect(ApolloWebSocketClosedException(code ?: CLOSE_NORMAL, reason))
  }

  enum class AddResult {
    Added,
    AlreadyExists,
    AlreadyClosed,
  }
  fun <D : Operation.Data> startOperation(request: ApolloRequest<D>, listener: WebSocketOperationListener) {
    val added = lock.withLock {
      idleJob?.cancel()
      idleJob = null

      if (state == State.Disconnected) {
        AddResult.AlreadyClosed
      } else if (activeListeners.containsKey(request.requestUuid.toString())){
        AddResult.AlreadyExists
      } else if (state != State.Connected) {
        activeListeners.put(request.requestUuid.toString(), ActiveOperationListener(listener, false))
        pending.add(request)
        AddResult.Added
      } else {
        activeListeners.put(request.requestUuid.toString(), ActiveOperationListener(listener, false))
        scope.launch { webSocket.send(wsProtocol.operationStart(request)) }
        AddResult.Added
      }
    }

    when (added) {
      AddResult.AlreadyClosed -> listener.onError(throwable!!)
      AddResult.AlreadyExists -> listener.onError(DefaultApolloException("There is already a subscription with that id"))
      AddResult.Added -> Unit
    }
  }

  fun <D : Operation.Data> stopOperation(request: ApolloRequest<D>, listener: WebSocketOperationListener) {
    val id = request.requestUuid.toString()
    val removed = lock.withLock {
      var ret = false
      if (activeListeners.containsKey(id) && activeListeners.get(id)?.operationListener == listener) {
        activeListeners.remove(id)
        ret = true
      }
      if (activeListeners.isEmpty()) {
        idleJob?.cancel()
        idleJob = scope.launch {
          delay(idleTimeoutMillis)
          /**
           * There is a small race condition here that [startOperation] might be enqueued concurrently with this
           * code and therefore the operation will fail instantly. This is in general true for any network
           * operation as the socket can error at any time but this one we have control over.
           * If that ever becomes an issue, one mitigation is to allow [startOperation] to fail and retry it later.
           */
          disconnect(DefaultApolloException("WebSocket is idle"))
        }
      }
      ret
    }

    if (!removed) {
      return
    }

    scope.launch { webSocket.send(wsProtocol.operationStop(request)) }
  }

  fun closeConnection(throwable: Throwable) {
    disconnect(throwable)
  }

  internal fun cancel() {
    webSocket.close(CLOSE_GOING_AWAY, "Cancelled")
    scope.cancel()
  }
}

private enum class State {
  Initial,
  AwaitAck,
  Connected,
  Disconnected

}

private fun WebSocket.send(clientMessage: ClientMessage) {
  when (clientMessage) {
    is TextClientMessage -> send(clientMessage.text)
    is DataClientMessage -> send(clientMessage.data)
  }
}

/**
 * @param terminated a flag that signals that a general error happened and all future WebSocket messages
 * must be ignored. This is to make it robust to [WsProtocol] that send spurious messages.
 */
private class ActiveOperationListener(val operationListener: WebSocketOperationListener, var terminated: Boolean)

