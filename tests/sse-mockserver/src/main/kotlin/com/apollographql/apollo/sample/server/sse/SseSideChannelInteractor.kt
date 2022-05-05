package com.apollographql.apollo.sample.server.sse

import com.apollographql.apollo.sample.server.routing.SseTransportMessage

class SseSideChannelInteractor {
  fun processRequest(request: SseTransportMessage.ClientRequest): SseTransportMessage.ClientResponse {
    return SseTransportMessage.ClientResponse(type = "hello")
  }
}
