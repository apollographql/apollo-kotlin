package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import reactor.core.publisher.Mono


@Component
class RootQuery : Query {
  fun random(): Int = 42
  fun time(): Int = 0

  @GraphQLDescription("Closes the socket")
  suspend fun closeWebSocket(): String {
    val sessions = MyWebSocketHandler.sessions()
    sessions.forEach {
      println("${System.currentTimeMillis()}: closing session ${it.key}...")
      it.value.close(CloseStatus.SERVER_ERROR).block()
      println("${System.currentTimeMillis()}: session closed.")
    }
    return "Closed ${sessions.size} session(s)"
  }
}

