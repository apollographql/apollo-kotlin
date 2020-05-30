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
import com.apollographql.apollo.network.websocket.ApolloGraphQLServerMessage.Companion.parse
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
    private val webSocketFactory: WebSocketFactory,
    private val connectionParams: Map<String, Any?> = emptyMap(),
    private val connectionAcknowledgeTimeoutMs: Long = 10_000
) : NetworkTransport {
  private val mutex = Mutex()
  private var graphQLWebsocketConnection: GraphQLWebsocketConnection? = null

  override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
    return getServerConnection().flatMapLatest { serverConnection ->
      serverConnection
          .openSubscription()
          .consumeAsFlow()
          .map { data -> data.parse() }
          .filter { message -> message !is ApolloGraphQLServerMessage.ConnectionAcknowledge }
          .takeWhile { message -> message !is ApolloGraphQLServerMessage.Complete || message.id != request.uuid.toString() }
          .map { message -> message.process(request.uuid) }
          .filterNotNull()
          .onStart {
            serverConnection.send(
                ApolloGraphQLClientMessage.Start(request)
            )
          }.onCompletion { cause ->
            if (cause != null) {
              serverConnection.send(
                  ApolloGraphQLClientMessage.Stop(request.uuid)
              )
            }
          }
    }
  }

  private fun ApolloGraphQLServerMessage.process(requestUuid: Uuid): GraphQLResponse? {
    return when (this) {
      is ApolloGraphQLServerMessage.Error -> {
        if (id == requestUuid.toString()) {
          throw ApolloWebSocketServerException(
              message = "Failed to execute GraphQL operation",
              payload = payload
          )
        }
        null
      }

      is ApolloGraphQLServerMessage.Data -> {
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

  private fun getServerConnection(): Flow<GraphQLWebsocketConnection> {
    return flow {
      val connection = mutex.withLock {
        if (graphQLWebsocketConnection?.isClosedForReceive != false) {
          graphQLWebsocketConnection = openServerConnection()
        }
        graphQLWebsocketConnection
      }
      emit(connection)
    }.filterNotNull()
  }

  private suspend fun openServerConnection(): GraphQLWebsocketConnection {
    return try {
      withTimeout(connectionAcknowledgeTimeoutMs) {
        val webSocketConnection = webSocketFactory.open()
        webSocketConnection.send(ApolloGraphQLClientMessage.Init(connectionParams).serialize())
        while (webSocketConnection.receive().parse() !is ApolloGraphQLServerMessage.ConnectionAcknowledge) {
          // await for connection acknowledgement
        }
        GraphQLWebsocketConnection(webSocketConnection)
      }
    } catch (e: TimeoutCancellationException) {
      throw ApolloWebSocketException(
          message = "Failed to establish GraphQL websocket connection with the server, timeout.",
          cause = e
      )
    } catch (e: Exception) {
      throw ApolloWebSocketException(
          message = "Failed to establish GraphQL websocket connection with the server.",
          cause = e
      )
    }
  }

  private class GraphQLWebsocketConnection(
      val webSocketConnection: WebSocketConnection
  ) : BroadcastChannel<ByteString> by webSocketConnection.broadcast(Channel.CONFLATED) {

    val isClosedForReceive: Boolean
      get() = webSocketConnection.isClosedForReceive

    fun send(message: ApolloGraphQLClientMessage) {
      webSocketConnection.send(message.serialize())
    }
  }
}
