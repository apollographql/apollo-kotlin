package com.apollographql.apollo3.network.websocket

sealed interface ServerMessage
object ConnectionAckServerMessage : ServerMessage
object ConnectionKeepAliveServerMessage : ServerMessage
object PingServerMessage : ServerMessage
object PongServerMessage : ServerMessage
class ConnectionErrorServerMessage(val payload: Any?) : ServerMessage

/**
 * A GraphQL response was received
 *
 * @param response, a GraphQL response, possibly containing errors.
 * @param complete, whether this is a terminal message for the given operation.
 */
class ResponseServerMessage(val id: String, val response: Any?, val complete: Boolean) : ServerMessage

/**
 * The subscription completed normally
 * This is a terminal message for the given operation
 */
class CompleteServerMessage(val id: String) : ServerMessage

/**
 * There was an error with the operation that cannot be represented by a GraphQL response
 *
 * This is a terminal message for the given operation.
 *
 * @param payload additional information regarding the error. It may represent a GraphQL error
 * but it doesn't have to
 */
class OperationErrorServerMessage(val id: String, val payload: Any?) : ServerMessage

/**
 * There was an error with the server that terminates all operations
 *
 * This is a terminal message for the all operations
 */
class GeneralErrorServerMessage(val exception: Exception) : ServerMessage

/**
 * Special Server message that indicates a malformed message
 */
class ParseErrorServerMessage(val errorMessage: String) : ServerMessage
