package com.apollographql.apollo.network.ws

import com.apollographql.apollo.exception.ApolloWebSocketException
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.Utils.readRecursively
import okio.Buffer
import okio.ByteString

sealed class ApolloGraphQLServerMessage {
  class ConnectionError(val rawMessage: String?) : ApolloGraphQLServerMessage() {
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
        val messageData = jsonReader.readRecursively() as? Map<String, Any?> ?: emptyMap()
        object {
          val id = messageData["id"] as String?
          val type = messageData["type"] as String?
          val payload = messageData["payload"]
        }
      } catch (e: Exception) {
        throw ApolloWebSocketException(
            message = "Failed to parse server message",
            cause = e
        )
      }

      return try {
        when (message.type) {
          ConnectionError.TYPE -> ConnectionError(message.payload?.toString())
          ConnectionAcknowledge.TYPE -> ConnectionAcknowledge
          Data.TYPE -> Data(message.id!!, message.payload as Map<String, Any?>?
              ?: emptyMap())
          Error.TYPE -> Error(message.id!!, message.payload as Map<String, Any?>?
              ?: emptyMap())
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
