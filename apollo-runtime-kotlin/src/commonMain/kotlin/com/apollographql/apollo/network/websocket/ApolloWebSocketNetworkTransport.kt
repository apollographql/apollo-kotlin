package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.ApolloWebSocketServerException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.websocket.GraphQLServerMessage.Companion.parse
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okio.Buffer

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloWebSocketNetworkTransport(
    private val connectionAcknowledgeTimeoutMs: Long = 10_000,
    private val connectionParams: Map<String, Any?>,
    private val apolloWebSocketFactory: ApolloWebSocketFactory
) : NetworkTransport {
  private val mutex = Mutex()
  private var serverServerConnection: ServerConnection? = null

  override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
    val requestUuid = uuid4()
    return getServerConnection().flatMapLatest { serverConnection ->
      flow {
        coroutineScope {
          serverConnection.collect { message ->
            process(message = message, requestUuid = requestUuid)?.also {
              emit(it)
            }
          }
        }
      }.onStart {
        serverConnection.send(
            GraphQLClientMessage.Start(
                uuid = requestUuid,
                request = request
            )
        )
      }.onCompletion {
        serverConnection.send(
            GraphQLClientMessage.Stop(
                uuid = requestUuid
            )
        )
      }
    }
  }

  private fun CoroutineScope.process(message: GraphQLServerMessage, requestUuid: Uuid): GraphQLResponse? {
    return when (message) {
      is GraphQLServerMessage.Complete -> {
        if (message.id == requestUuid.toString()) {
          cancel()
        }
        null
      }

      is GraphQLServerMessage.Error -> {
        if (message.id == requestUuid.toString()) {
          throw ApolloWebSocketServerException(
              message = "Failed to execute GraphQL operation",
              payload = message.payload
          )
        }
        null
      }

      is GraphQLServerMessage.Data -> {
        if (message.id == requestUuid.toString()) {
          val buffer = Buffer()
          JsonWriter.of(buffer)
              .apply { Utils.writeToJson(message.payload, this) }
              .flush()
          GraphQLResponse(
              body = buffer,
              executionContext = ExecutionContext.Empty
          )
        } else null
      }

      else -> null
    }
  }

  private fun getServerConnection(): Flow<ServerConnection> {
    return flow {
      val connection = mutex.withLock {
        if (serverServerConnection == null) {
          serverServerConnection = openServerConnection().apply {
            onCompletion {
              mutex.withLock {
                serverServerConnection = null
              }
            }
          }
        }
        serverServerConnection
      }
      emit(connection!!)
    }
  }

  private suspend fun openServerConnection(): ServerConnection {
    val webSocketConnection = apolloWebSocketFactory.open()
    val eventChannel = webSocketConnection.broadcast(Channel.CONFLATED)

    withTimeout(connectionAcknowledgeTimeoutMs) {
      eventChannel.awaitOpen()
      eventChannel.awaitConnectionAcknowledge(webSocketConnection)
    }

    val serverMessageFlow = flow {
      val channel = eventChannel.openSubscription()
      try {
        for (event in channel) {
          if (event is ApolloWebSocketConnection.Event.Message) {
            emit(event.data.parse())
          }
        }
      } finally {
        channel.cancel()
      }
    }
    return ServerConnection(
        webSocketConnection = webSocketConnection,
        serverMessageFlow = serverMessageFlow
    )
  }

  private suspend fun BroadcastChannel<ApolloWebSocketConnection.Event>.awaitOpen() {
    openSubscription()
        .consumeAsFlow()
        .filterIsInstance<ApolloWebSocketConnection.Event.Open>()
        .firstOrNull()
        ?: throw ApolloWebSocketException("Failed to open web socket connection")
  }

  private suspend fun BroadcastChannel<ApolloWebSocketConnection.Event>.awaitConnectionAcknowledge(
      webSocketConnection: ApolloWebSocketConnection
  ) {
    openSubscription()
        .consumeAsFlow()
        .filterIsInstance<ApolloWebSocketConnection.Event.Message>()
        .filter { event ->
          val message = event.data.parse()
          message is GraphQLServerMessage.ConnectionAcknowledge
        }
        .onStart { webSocketConnection.send(GraphQLClientMessage.Init(connectionParams).serialize()) }
        .firstOrNull()
        ?: throw ApolloWebSocketException("Failed to open web socket connection")
  }

  private class ServerConnection(
      private val webSocketConnection: ApolloWebSocketConnection,
      private val serverMessageFlow: Flow<GraphQLServerMessage>
  ) : Flow<GraphQLServerMessage> by serverMessageFlow {
    fun send(message: GraphQLClientMessage) {
      webSocketConnection.send(message.serialize())
    }
  }
}
