package com.apollographql.apollo3.execution.websocket

import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.execution.ApolloWebsocketClientMessageParseError
import com.apollographql.apollo3.execution.ApolloWebsocketComplete
import com.apollographql.apollo3.execution.ApolloWebsocketConnectionAck
import com.apollographql.apollo3.execution.ApolloWebsocketConnectionError
import com.apollographql.apollo3.execution.ApolloWebsocketData
import com.apollographql.apollo3.execution.ApolloWebsocketError
import com.apollographql.apollo3.execution.ApolloWebsocketInit
import com.apollographql.apollo3.execution.ApolloWebsocketServerMessage
import com.apollographql.apollo3.execution.ApolloWebsocketStart
import com.apollographql.apollo3.execution.ApolloWebsocketStop
import com.apollographql.apollo3.execution.ApolloWebsocketTerminate
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.SubscriptionItemError
import com.apollographql.apollo3.execution.SubscriptionItemResponse
import com.apollographql.apollo3.execution.WebSocketBinaryMessage
import com.apollographql.apollo3.execution.WebSocketHandler
import com.apollographql.apollo3.execution.WebSocketMessage
import com.apollographql.apollo3.execution.WebSocketTextMessage
import com.apollographql.apollo3.execution.parseApolloWebsocketClientMessage
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.Buffer

private fun ApolloWebsocketServerMessage.toWsMessage(): WebSocketMessage {
  return WebSocketTextMessage(Buffer().apply { serialize(this) }.readUtf8())
}

sealed interface ConnectionInitResult
object ConnectionInitAck : ConnectionInitResult
class ConnectionInitError(val payload: Optional<Any?> = Optional.absent()): ConnectionInitResult

typealias ConnectionInitHandler = suspend (Any?) -> ConnectionInitResult

private class CurrentSubscription(val id: String) : ExecutionContext.Element {

  override val key: ExecutionContext.Key<CurrentSubscription> = Key

  companion object Key : ExecutionContext.Key<CurrentSubscription>
}

fun ExecutionContext.subscriptionId(): String = get(CurrentSubscription)?.id ?: error("Apollo: not executing a subscription")

class ApolloWebSocketHandler(
    private val executableSchema: ExecutableSchema,
    private val scope: CoroutineScope,
    private val executionContext: ExecutionContext,
    private val sendMessage: (WebSocketMessage) -> Unit,
    private val connectionInitHandler: ConnectionInitHandler = { ConnectionInitAck },
) : WebSocketHandler {
  private val lock = reentrantLock()
  private val activeSubscriptions = mutableMapOf<String, Job>()
  private var isClosed: Boolean = false
  private var initJob: Job? = null

  override fun handleMessage(message: WebSocketMessage) {
    val clientMessage = when (message) {
      is WebSocketBinaryMessage -> message.data.decodeToString()
      is WebSocketTextMessage -> message.data
    }.parseApolloWebsocketClientMessage()

    when (clientMessage) {
      is ApolloWebsocketInit -> {
        initJob = lock.withLock {
          scope.launch {
            when(val result = connectionInitHandler.invoke(clientMessage.connectionParams)) {
              is ConnectionInitAck -> {
                sendMessage(ApolloWebsocketConnectionAck.toWsMessage())
              }
              is ConnectionInitError -> {
                sendMessage(ApolloWebsocketConnectionError(result.payload).toWsMessage())
              }
            }
          }
        }
      }

      is ApolloWebsocketStart -> {
        val isActive = lock.withLock {
          activeSubscriptions.containsKey(clientMessage.id)
        }
        if (isActive) {
          sendMessage(ApolloWebsocketError(id = clientMessage.id, error = Error.Builder("Subscription ${clientMessage.id} is already active").build()).toWsMessage())
          return
        }

        val flow = executableSchema.executeSubscription(clientMessage.request, executionContext + CurrentSubscription(clientMessage.id))

        val job = scope.launch {
          flow.collect {
            when (it) {
              is SubscriptionItemResponse -> {
                sendMessage(ApolloWebsocketData(id = clientMessage.id, response = it.response).toWsMessage())
              }

              is SubscriptionItemError -> {
                sendMessage(ApolloWebsocketError(id = clientMessage.id, error = it.error).toWsMessage())
              }
            }
          }
          sendMessage(ApolloWebsocketComplete(id = clientMessage.id).toWsMessage())
          lock.withLock {
            activeSubscriptions.remove(clientMessage.id)?.cancel()
          }
        }

        lock.withLock {
          activeSubscriptions.put(clientMessage.id, job)
        }
      }

      is ApolloWebsocketStop -> {
        lock.withLock {
          activeSubscriptions.remove(clientMessage.id)?.cancel()
        }
      }

      ApolloWebsocketTerminate -> {
        // nothing to do
      }

      is ApolloWebsocketClientMessageParseError -> {
        sendMessage(ApolloWebsocketError(null, Error.Builder("Cannot handle message (${clientMessage.message})").build()).toWsMessage())
      }
    }
  }

  fun close() {
    lock.withLock {
      if (isClosed) {
        return
      }

      activeSubscriptions.forEach {
        it.value.cancel()
      }
      activeSubscriptions.clear()

      initJob?.cancel()
      isClosed = true
    }
  }
}