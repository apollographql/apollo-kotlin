
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.network.websocket.ClientMessage
import com.apollographql.apollo3.network.websocket.CompleteServerMessage
import com.apollographql.apollo3.network.websocket.ConnectionAckServerMessage
import com.apollographql.apollo3.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo3.network.websocket.ParseErrorServerMessage
import com.apollographql.apollo3.network.websocket.PingServerMessage
import com.apollographql.apollo3.network.websocket.PongServerMessage
import com.apollographql.apollo3.network.websocket.ResponseServerMessage
import com.apollographql.apollo3.network.websocket.ServerMessage
import com.apollographql.apollo3.network.websocket.SubscriptionParser
import com.apollographql.apollo3.network.websocket.SubscriptionParserFactory
import com.apollographql.apollo3.network.websocket.WsProtocol

actual val parserFactory: SubscriptionParserFactory?
  get() = JsSubscriptionParserFactory


object JsSubscriptionParserFactory : SubscriptionParserFactory {
  override fun <D : Operation.Data> createParser(request: ApolloRequest<D>): SubscriptionParser<D> {
    return object : SubscriptionParser<D> {
      override fun parse(payload: ApolloJsonElement): ApolloResponse<D> {
        return ApolloResponse.Builder(request.operation, request.requestUuid)
            .data(payload.asDynamic().data.unsafeCast<D>())
            .errors(null) // TODO: Error doesn't use @JsExport
            .build()
      }
    }
  }
}

actual val wsProtocol: WsProtocol?
  get() = JsWsProtocol

object JsWsProtocol : WsProtocol {
  private val delegate = GraphQLWsProtocol({ null })
  override val name: String
    get() = delegate.name

  override suspend fun connectionInit(): ClientMessage {
    return delegate.connectionInit()
  }

  override suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage {
    return delegate.operationStart(request)
  }

  override fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage {
    return delegate.operationStop(request)
  }

  override fun ping(): ClientMessage {
    return delegate.ping()
  }

  override fun pong(): ClientMessage {
    return delegate.pong()
  }

  override fun parseServerMessage(text: String): ServerMessage {
    val map:dynamic = JSON.parse(text)

    val type = map.type
    if (type == null) {
      return ParseErrorServerMessage("No 'type' found in server message: '$text'")
    }

    return when (type) {
      "connection_ack" -> ConnectionAckServerMessage
      "ping" -> PingServerMessage
      "pong" -> PongServerMessage
      "next", "complete", "error" -> {
        val id = map.id
        when {
          id == null -> ParseErrorServerMessage("No 'id' found in message: '$text'")
          type == "next" -> ResponseServerMessage(id, map.payload, false)
          type == "complete" -> CompleteServerMessage(id)
          type == "error" -> ResponseServerMessage(id, map.payload, true)
          else -> error("") // make the compiler happy
        }
      }

      else -> ParseErrorServerMessage("Unknown type: '$type' found in server message: '$text'")
    }
  }
}