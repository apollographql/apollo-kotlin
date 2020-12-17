package com.apollographql.apollo.subscription

import okio.Buffer

sealed class OperationServerMessage {
  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This method should be used any more. Instead you should use a serializer")
    fun fromJsonString(json: String): OperationServerMessage =
        ApolloOperationMessageSerializer.readServerMessage(Buffer().writeUtf8(json))
  }

  class ConnectionError(@JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "connection_error"
    }
  }

  class ConnectionAcknowledge : OperationServerMessage() {
    companion object {
      const val TYPE = "connection_ack"
    }
  }

  class Data(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "data"
    }
  }

  class Error(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "error"
    }
  }

  class Complete(@JvmField val id: String?) : OperationServerMessage() {
    companion object {
      const val TYPE = "complete"
    }
  }

  class ConnectionKeepAlive : OperationServerMessage() {
    companion object {
      const val TYPE = "ka"
    }
  }

  class Unsupported(@JvmField val rawMessage: String) : OperationServerMessage()
}