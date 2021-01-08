package com.apollographql.apollo.subscription

import okio.Buffer

sealed class OperationServerMessage {
  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"
  }

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

  class Unsupported(val rawMessage: String) : OperationServerMessage() {
    override fun equals(other: Any?): Boolean = other is Unsupported && other.rawMessage == rawMessage
    override fun hashCode(): Int = rawMessage.hashCode()
  }
}