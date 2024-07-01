package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeAny

/**
 * A WebSocket [message](https://datatracker.ietf.org/doc/html/rfc6455#section-1.2) sent by the client
 */
@ApolloExperimental
sealed interface ClientMessage

/**
 * A WebSocket text message
 */
@ApolloExperimental
class TextClientMessage(val text: String): ClientMessage
/**
 * A WebSocket data message
 */
@ApolloExperimental
class DataClientMessage(val data: ByteArray): ClientMessage

internal fun Any?.toClientMessage(): ClientMessage {
  return buildJsonString {
    writeAny(this@toClientMessage)
  }.let { TextClientMessage(it) }
}