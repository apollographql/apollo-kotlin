package com.apollographql.apollo3.network.sse

/**
 * Creates client requests
 */
open class SseTransportMessageFactory(private val messageType: SseTransportMessage.MessageType = SseTransportMessage.MessageType()) {

  var type: String? = null

  fun setTypeToInit(): SseTransportMessageFactory {
    type = messageType.initRequest
    return this
  }

  fun setTypeToStart(): SseTransportMessageFactory {
    type = messageType.startRequest
    return this
  }

  open fun build(): SseTransportMessage {
    checkNotNull(type) {
      "Apollo: 'type' is required"
    }

    return SseTransportMessage
        .ClientRequest(type = type!!)
  }
}
