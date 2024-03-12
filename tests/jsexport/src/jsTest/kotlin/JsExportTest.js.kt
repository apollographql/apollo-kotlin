
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.network.ws.incubating.ClientMessage
import com.apollographql.apollo3.network.ws.incubating.CompleteServerMessage
import com.apollographql.apollo3.network.ws.incubating.ConnectionAckServerMessage
import com.apollographql.apollo3.network.ws.incubating.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.incubating.ParseErrorServerMessage
import com.apollographql.apollo3.network.ws.incubating.PingServerMessage
import com.apollographql.apollo3.network.ws.incubating.PongServerMessage
import com.apollographql.apollo3.network.ws.incubating.ResponseServerMessage
import com.apollographql.apollo3.network.ws.incubating.ServerMessage
import com.apollographql.apollo3.network.ws.incubating.SubscriptionParser
import com.apollographql.apollo3.network.ws.incubating.SubscriptionParserFactory
import com.apollographql.apollo3.network.ws.incubating.WsProtocol

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