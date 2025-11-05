package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.json.ApolloJsonElement

/**
 * A WebSocket [message](https://datatracker.ietf.org/doc/html/rfc6455#section-1.2) sent by the server
 */
sealed interface ServerMessage
object ConnectionAckServerMessage : ServerMessage
object ConnectionKeepAliveServerMessage : ServerMessage
object PingServerMessage : ServerMessage
object PongServerMessage : ServerMessage
class ConnectionErrorServerMessage(val payload: ApolloJsonElement) : ServerMessage

/**
 * A GraphQL response was received
 *
 * @param response, a GraphQL response, possibly containing errors.
 */
class ResponseServerMessage(val id: String, val response: ApolloJsonElement) : ServerMessage

/**
 * The subscription completed normally
 * This is a terminal message for the given operation.
 */
class CompleteServerMessage(val id: String) : ServerMessage

/**
 * There was an error with the operation that cannot be represented by a GraphQL response.
 * This is a terminal message that terminates the subscription.
 *
 * @param payload additional information regarding the error.
 * It may represent a GraphQL error, but it doesn't have to.
 */
class OperationErrorServerMessage(val id: String, val payload: ApolloJsonElement) : ServerMessage

/**
 * Special Server message that indicates a malformed message
 */
class ParseErrorServerMessage(val errorMessage: String) : ServerMessage
