package com.apollographql.apollo.sample.server.sse

import com.apollographql.apollo3.network.sse.SseTransportMessage

/**
 * Proces
 */
class SseSideChannelInteractor(
    private val messageType: SseTransportMessage.MessageType = SseTransportMessage.MessageType(),
    private val sseTransportMessageFactory: SseServerTransportMessageFactory = SseServerTransportMessageFactory(),
) {
  fun processRequest(request: SseTransportMessage.ClientRequest): SseTransportMessage.ClientResponse {

    return when (request.type) {
      messageType.initRequest -> sseTransportMessageFactory.setMessageToAck()
      else -> throw RuntimeException("Apollo: cannot process message with type='${request.type}' ")
    }
        .build()

  }
}
