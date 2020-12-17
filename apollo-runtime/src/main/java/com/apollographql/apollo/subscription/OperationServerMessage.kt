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
    override fun hashCode(): Int = javaClass.hashCode()
    override fun equals(other: Any?): Boolean = other is ConnectionError

    companion object {
      const val TYPE = "connection_error"
    }
  }

  class ConnectionAcknowledge : OperationServerMessage() {
    override fun hashCode(): Int = javaClass.hashCode()
    override fun equals(other: Any?): Boolean = other is ConnectionAcknowledge

    companion object {
      const val TYPE = "connection_ack"
    }
  }

  data class Data(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "data"
    }
  }

  data class Error(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "error"
    }
  }

  data class Complete(@JvmField val id: String?) : OperationServerMessage() {
    companion object {
      const val TYPE = "complete"
    }
  }

  class ConnectionKeepAlive : OperationServerMessage() {
    override fun hashCode(): Int = javaClass.hashCode()
    override fun equals(other: Any?): Boolean = other is ConnectionKeepAlive

    companion object {
      const val TYPE = "ka"
    }
  }

  class Unsupported(@JvmField val rawMessage: String) : OperationServerMessage()
}