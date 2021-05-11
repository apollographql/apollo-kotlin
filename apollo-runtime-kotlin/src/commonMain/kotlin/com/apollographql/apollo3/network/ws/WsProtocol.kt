package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

interface WsProtocol {
  val frameType: WsFrameType
  val name: String
  fun connectionInit(): Map<String, Any?>
  fun connectionTerminate(): Map<String, Any?>?
  fun <D: Operation.Data> operationStart(request: ApolloRequest<D>): Map<String, Any?>
  fun <D: Operation.Data> operationStop(request: ApolloRequest<D>): Map<String, Any?>
  
  fun parseMessage(string: String): WsMessage
}

enum class WsFrameType {
  Text,
  Binary
}
sealed class WsMessage {
  object ConnectionAck : WsMessage()
  class ConnectionError(val payload: Map<String, Any?>?) : WsMessage()
  class OperationData(val id: String, val payload: Map<String, Any?>) : WsMessage()
  class OperationError(val id: String, val payload: Map<String, Any?>) : WsMessage()
  class OperationComplete(val id: String): WsMessage()
  object KeepAlive: WsMessage()
  class Unknown(val map: Map<String, Any?>): WsMessage()
}

