package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import okio.Buffer
import okio.ByteString

sealed class ApolloGraphQLServerMessage {
  class ConnectionError(val payload: Map<String, Any?>) : ApolloGraphQLServerMessage() {
    companion object {
      const val TYPE = "connection_error"
    }
  }

  object ConnectionAcknowledge : ApolloGraphQLServerMessage() {
    const val TYPE = "connection_ack"
  }

  class Data(val id: String, val payload: Map<String, Any?>) : ApolloGraphQLServerMessage() {
    companion object {
      const val TYPE = "data"
    }
  }

  class Error(val id: String, val payload: Map<String, Any?>) : ApolloGraphQLServerMessage() {
    companion object {
      const val TYPE = "error"
    }
  }

  class Complete(val id: String) : ApolloGraphQLServerMessage() {
    companion object {
      const val TYPE = "complete"
    }
  }

  object ConnectionKeepAlive : ApolloGraphQLServerMessage() {
    const val TYPE = "ka"
  }

  class Unsupported(val rawMessage: String) : ApolloGraphQLServerMessage()

  companion object {

    fun ByteString.parse(): ApolloGraphQLServerMessage {
      val message = try {
        val jsonReader = BufferedSourceJsonReader(Buffer().write(this))
        val responseJsonReader = ResponseJsonStreamReader(jsonReader)
        val messageData = responseJsonReader.toMap() ?: emptyMap()
        object {
          val id = messageData["id"] as String?
          val type = messageData["type"] as String?
          val payload = messageData["payload"] as Map<String, Any?>? ?: emptyMap()
        }
      } catch (e: Exception) {
        throw ApolloWebSocketException(
            message = "Failed to parse server message",
            cause = e
        )
      }

      return try {
        when (message.type) {
          ConnectionError.TYPE -> ConnectionError(message.payload)
          ConnectionAcknowledge.TYPE -> ConnectionAcknowledge
          Data.TYPE -> Data(message.id!!, message.payload)
          Error.TYPE -> Error(message.id!!, message.payload)
          Complete.TYPE -> Complete(message.id!!)
          ConnectionKeepAlive.TYPE -> ConnectionKeepAlive
          else -> Unsupported(utf8())
        }
      } catch (e: Exception) {
        throw ApolloWebSocketException(
            message = "Failed to parse server message",
            cause = e
        )
      }
    }
  }
}
