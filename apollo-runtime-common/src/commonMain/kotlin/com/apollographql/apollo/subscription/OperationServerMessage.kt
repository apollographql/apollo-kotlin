package com.apollographql.apollo.subscription

import okio.Buffer

sealed class OperationServerMessage {
  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"
  }

  data class ConnectionError(val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "connection_error"
    }
  }

  object ConnectionAcknowledge : OperationServerMessage() {
    const val TYPE = "connection_ack"
  }

  data class Data(val id: String?, val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "data"
    }
  }

  data class Error(val id: String?, val payload: Map<String, Any?>) : OperationServerMessage() {
    companion object {
      const val TYPE = "error"
    }
  }

  data class Complete(val id: String?) : OperationServerMessage() {
    companion object {
      const val TYPE = "complete"
    }
  }

  object ConnectionKeepAlive : OperationServerMessage() {
    const val TYPE = "ka"
  }

  data class Unsupported(val rawMessage: String) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Unsupported && other.rawMessage == rawMessage
    override fun hashCode(): Int = rawMessage.hashCode()
  }
}