package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.ApolloJsonElement

/**
 * A WebSocket [message](https://datatracker.ietf.org/doc/html/rfc6455#section-1.2) sent by the server
 */
@ApolloExperimental
sealed interface ServerMessage
@ApolloExperimental
object ConnectionAckServerMessage : ServerMessage
@ApolloExperimental
object ConnectionKeepAliveServerMessage : ServerMessage
@ApolloExperimental
object PingServerMessage : ServerMessage
@ApolloExperimental
object PongServerMessage : ServerMessage
@ApolloExperimental
class ConnectionErrorServerMessage(val payload: ApolloJsonElement) : ServerMessage

/**
 * A GraphQL response was received
 *
 * @param response, a GraphQL response, possibly containing errors.
 * @param complete, whether this is a terminal message for the given operation.
 */
@ApolloExperimental
class ResponseServerMessage(val id: String, val response: ApolloJsonElement, val complete: Boolean) : ServerMessage

/**
 * The subscription completed normally
 * This is a terminal message for the given operation.
 */
@ApolloExperimental
class CompleteServerMessage(val id: String) : ServerMessage

/**
 * There was an error with the operation that cannot be represented by a GraphQL response
 *
 * @param payload additional information regarding the error. It may represent a GraphQL error
 * but it doesn't have to
 */
@ApolloExperimental
class OperationErrorServerMessage(val id: String, val payload: ApolloJsonElement, val terminal: Boolean) : ServerMessage

/**
 * Special Server message that indicates a malformed message
 */
@ApolloExperimental
class ParseErrorServerMessage(val errorMessage: String) : ServerMessage
