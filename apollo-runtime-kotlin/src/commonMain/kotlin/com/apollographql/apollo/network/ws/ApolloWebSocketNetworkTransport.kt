package com.apollographql.apollo.network.ws

import com.apollographql.apollo.ApolloParseException
import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.ApolloWebSocketServerException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloResponse
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.ws.ApolloGraphQLServerMessage.Companion.parse
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
import kotlinx.coroutines.flow.mapNotNull
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
/**
 * Apollo GraphQL WS protocol implementation:
 * https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
 */
class ApolloWebSocketNetworkTransport(
    private val webSocketFactory: WebSocketFactory,
    private val connectionParams: Map<String, Any?> = emptyMap(),
    private val connectionAcknowledgeTimeoutMs: Long = 10_000
) : NetworkTransport {
  private val mutex = Mutex()
  private var graphQLWebsocketConnection: GraphQLWebsocketConnection? = null

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>, executionContext: ExecutionContext): Flow<ApolloResponse<D>> {
    return getServerConnection().flatMapLatest { serverConnection ->
      serverConnection
          .openSubscription()
          .consumeAsFlow()
          .map { data -> data.parse() }
          .filter { message -> message !is ApolloGraphQLServerMessage.ConnectionAcknowledge }
          .takeWhile { message -> message !is ApolloGraphQLServerMessage.Complete || message.id != request.requestUuid.toString() }
          .mapNotNull { message -> message.process(request) }
          .onStart {
            serverConnection.send(
                ApolloGraphQLClientMessage.Start(request)
            )
          }.onCompletion { cause ->
            if (cause == null) {
              serverConnection.send(
                  ApolloGraphQLClientMessage.Stop(request.requestUuid)
              )
            }
          }
    }
  }

  private fun <D : Operation.Data> ApolloGraphQLServerMessage.process(request: ApolloRequest<D>): ApolloResponse<D>? {
    return when (this) {
      is ApolloGraphQLServerMessage.Error -> {
        if (id == request.requestUuid.toString()) {
          throw ApolloWebSocketServerException(
              message = "Failed to execute GraphQL operation",
              payload = payload
          )
        }
        null
      }

      is ApolloGraphQLServerMessage.Data -> {
        if (id == request.requestUuid.toString()) {
          val buffer = Buffer().apply {
            JsonWriter.of(buffer)
                .apply { Utils.writeToJson(payload, this) }
                .flush()
          }

          val response = try {
            request.operation.parse(
                source = buffer,
                scalarTypeAdapters = request.scalarTypeAdapters
            )
          } catch (e: Exception) {
            throw ApolloParseException(
                message = "Failed to parse GraphQL network response",
                cause = e
            )
          }

          ApolloResponse(
              requestUuid = request.requestUuid,
              response = response,
              executionContext = ExecutionContext.Empty
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
        val webSocketConnection = webSocketFactory.open(
            mapOf(
                "Sec-WebSocket-Protocol" to "graphql-ws"
            )
        )
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
