package com.apollographql.apollo.sample.server.routing

import kotlinx.serialization.Serializable

// TODO these classes need to be moved into apollo-runtime commonMain com.apollographql.apollo3.network.sse
sealed class SseTransportMessage {

  @Serializable
  data class ClientRequest(
      val type: String,
  ) : SseTransportMessage()

  @Serializable
  data class ClientResponse(
      val type: String,
  ) : SseTransportMessage()
}
