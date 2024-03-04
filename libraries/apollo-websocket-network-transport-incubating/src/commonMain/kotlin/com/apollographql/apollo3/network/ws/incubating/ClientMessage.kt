package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny

sealed interface ClientMessage
class TextClientMessage(val text: String): ClientMessage
class DataClientMessage(val data: ByteArray): ClientMessage

internal fun Any?.toClientMessage(): ClientMessage {
  return buildJsonString {
    writeAny(this@toClientMessage)
  }.let { TextClientMessage(it) }
}