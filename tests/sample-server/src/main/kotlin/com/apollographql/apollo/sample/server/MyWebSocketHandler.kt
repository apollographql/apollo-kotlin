package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionProtocolHandler
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.boot.web.servlet.server.Session
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

class MyWebSocketHandler(
    private val subscriptionHandler: ApolloSubscriptionProtocolHandler,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {
  @ExperimentalCoroutinesApi
  @Suppress("ForbiddenVoid")
  override fun handle(session: WebSocketSession): Mono<Void> {
    val response = session.receive()
        .flatMap { subscriptionHandler.handle(it.payloadAsText, session) }
        .map {
          objectMapper.writeValueAsString(it)
        }
        .map {
          session.textMessage(it)
        }
        .doOnSubscribe {
          synchronized(activeSessions) {
            println("adding session $index")
            activeSessions.put(index++, session)
          }
        }
        .doFinally {
          synchronized(activeSessions) {
            val iterator = activeSessions.iterator()
            while (iterator.hasNext()) {
              val entry = iterator.next()
              if (entry.value == session) {
                println("removing session ${entry.key}")
                iterator.remove()
              }
            }
          }
        }

    return session.send(response)
  }

  override fun getSubProtocols(): List<String> = listOf("graphql-ws")

  companion object {
    var index = 0
    private val activeSessions = mutableMapOf<Int, WebSocketSession>()

    fun sessions() = synchronized(activeSessions) {
      val copy = mutableMapOf<Int, WebSocketSession>()
      copy.putAll(activeSessions)
      copy
    }
  }
}
