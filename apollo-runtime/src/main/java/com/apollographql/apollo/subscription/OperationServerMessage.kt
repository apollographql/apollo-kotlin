package com.apollographql.apollo.subscription

import okio.Buffer

sealed class OperationServerMessage {
  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This method is deprecated. Use an OperationMessageSerializer instead.")
    fun fromJsonString(json: String): OperationServerMessage =
        ApolloOperationMessageSerializer.readServerMessage(Buffer().writeUtf8(json))
  }

  class ConnectionError(@JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is ConnectionError && other.payload == payload
    override fun hashCode(): Int = payload.hashCode()

    companion object {
      const val TYPE = "connection_error"
    }
  }

  class ConnectionAcknowledge : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is ConnectionAcknowledge
    override fun hashCode(): Int = javaClass.hashCode()

    companion object {
      const val TYPE = "connection_ack"
    }
  }

  class Data(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Data && other.id == id && other.payload == payload
    override fun hashCode(): Int = 31 * (id?.hashCode() ?: 0) + payload.hashCode()

    companion object {
      const val TYPE = "data"
    }
  }

  class Error(@JvmField val id: String?, @JvmField val payload: Map<String, Any?>) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Error && other.id == id && other.payload == payload
    override fun hashCode(): Int = 31 * (id?.hashCode() ?: 0) + payload.hashCode()

    companion object {
      const val TYPE = "error"
    }
  }

  class Complete(@JvmField val id: String?) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Complete && other.id == id
    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
      const val TYPE = "complete"
    }
  }

  class ConnectionKeepAlive : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is ConnectionKeepAlive
    override fun hashCode(): Int = javaClass.hashCode()

    companion object {
      const val TYPE = "ka"
    }
  }

  class Unsupported(@JvmField val rawMessage: String) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Unsupported && other.rawMessage == rawMessage
    override fun hashCode(): Int = rawMessage.hashCode()
  }
}