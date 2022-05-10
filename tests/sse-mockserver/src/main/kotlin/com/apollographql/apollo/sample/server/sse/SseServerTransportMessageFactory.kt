package com.apollographql.apollo.sample.server.sse

import com.apollographql.apollo3.network.sse.SseTransportMessage
import com.apollographql.apollo3.network.sse.SseTransportMessageFactory

open class SseServerTransportMessageFactory(private val messageType: SseTransportMessage.MessageType = SseTransportMessage.MessageType())
  : SseTransportMessageFactory(messageType) {

  fun setMessageToAck(): SseServerTransportMessageFactory {
    type = messageType.acknowledgeResponse
    return this
  }

  override fun build(): SseTransportMessage.ClientResponse {
    checkNotNull(type) {
      "Apollo: 'type' is required"
    }

    return SseTransportMessage.ClientResponse(
        type = type!!
    )
  }
}