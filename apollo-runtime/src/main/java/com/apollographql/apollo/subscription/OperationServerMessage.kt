package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import okio.Buffer
import java.io.IOException
import java.util.Collections

sealed class OperationServerMessage {
  class ConnectionError(val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "connection_error"
    }
  }

  class ConnectionAcknowledge : OperationServerMessage() {
    companion object {
      const val TYPE = "connection_ack"
    }
  }

  class Data(val id: String?, val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "data"
    }
  }

  class Error(val id: String?, val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "error"
    }
  }

  class Complete(val id: String?) : OperationServerMessage() {
    companion object {
      const val TYPE = "complete"
    }
  }

  class ConnectionKeepAlive : OperationServerMessage() {
    companion object {
      const val TYPE = "ka"
    }
  }

  class Unsupported(val rawMessage: String) : OperationServerMessage()

  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"

    fun fromJsonString(json: String): OperationServerMessage =
        try {
          readFromJson(BufferedSourceJsonReader(Buffer().writeUtf8(json)))
        } catch (e: Exception) {
          Unsupported(json)
        }

    @Throws(IOException::class)
    private fun readFromJson(reader: JsonReader): OperationServerMessage {
      val responseJsonStreamReader = ResponseJsonStreamReader(reader)
      val messageData = requireNotNull(responseJsonStreamReader.toMap())
      val id = messageData[JSON_KEY_ID] as String?
      return when (val type = messageData[JSON_KEY_TYPE] as String?) {
        ConnectionError.TYPE -> ConnectionError(messagePayload(messageData))
        ConnectionAcknowledge.TYPE -> ConnectionAcknowledge()
        Data.TYPE -> Data(id, messagePayload(messageData))
        Error.TYPE -> Error(id, messagePayload(messageData))
        Complete.TYPE -> Complete(id)
        ConnectionKeepAlive.TYPE -> ConnectionKeepAlive()
        else -> throw IOException("Unsupported message type $type")
      }
    }

    private fun messagePayload(messageData: Map<String, Any?>): Map<String, Any> =
        @Suppress("UNCHECKED_CAST")
        (messageData[JSON_KEY_PAYLOAD] as Map<String, Any>?)
            ?.let { Collections.unmodifiableMap(it) }
            ?: emptyMap()
  }
}