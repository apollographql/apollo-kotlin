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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.ByteString

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloWebSocketNetworkTransport(
    private val webSocketFactory: ApolloWebSocketFactory,
    private val connectionParams: Map<String, Any?> = emptyMap(),
    private val connectionAcknowledgeTimeoutMs: Long = 10_000
) : NetworkTransport {
  private val mutex = Mutex()
  private var serverServerConnection: ServerConnection? = null

  override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
    return getServerConnection().flatMapLatest { serverConnection ->
      serverConnection
          .takeWhile { message -> message !is GraphQLServerMessage.Complete || message.id != request.uuid.toString() }
          .map { message -> message.process(request.uuid) }
          .filterNotNull()
          .onStart {
            serverConnection.send(
                GraphQLClientMessage.Start(request)
            )
          }.onCompletion { cause ->
            if (cause != null) {
              serverConnection.send(
                  GraphQLClientMessage.Stop(request.uuid)
              )
            }
          }
    }
  }

  private fun GraphQLServerMessage.process(requestUuid: Uuid): GraphQLResponse? {
    return when (this) {
      is GraphQLServerMessage.Error -> {
        if (id == requestUuid.toString()) {
          throw ApolloWebSocketServerException(
              message = "Failed to execute GraphQL operation",
              payload = payload
          )
        }
        null
      }

      is GraphQLServerMessage.Data -> {
        if (id == requestUuid.toString()) {
          val buffer = Buffer()
          JsonWriter.of(buffer)
              .apply { Utils.writeToJson(payload, this) }
              .flush()
          GraphQLResponse(
              body = buffer,
              executionContext = ExecutionContext.Empty,
              requestUuid = requestUuid
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
    val (webSocketConnection, eventChannel) = try {
      val webSocketConnection = webSocketFactory.open()
      val eventChannel = webSocketConnection.broadcast(Channel.CONFLATED)
      withTimeout(connectionAcknowledgeTimeoutMs) {
        eventChannel.awaitConnectionAcknowledge(webSocketConnection)
      }
      webSocketConnection to eventChannel
    } catch (e: TimeoutCancellationException) {
      throw ApolloWebSocketException(
          message = "Failed to establish GraphQL web socket connection with the server, timeout.",
          cause = e
      )
    }

    val serverMessageFlow = eventChannel.openSubscription()
        .consumeAsFlow()
        .map { it.parse() }
        .filter { it !is GraphQLServerMessage.ConnectionAcknowledge }

    return ServerConnection(
        webSocketConnection = webSocketConnection,
        serverMessageFlow = serverMessageFlow
    )
  }

  private suspend fun BroadcastChannel<ByteString>.awaitConnectionAcknowledge(
      webSocketConnection: ApolloWebSocketConnection
  ) {
    openSubscription()
        .consumeAsFlow()
        .filter { messageData ->
          val message = messageData.parse()
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
