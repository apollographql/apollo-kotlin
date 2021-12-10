package com.apollographql.apollo.sample.server

import org.springframework.web.reactive.HandlerResult
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class MyWebSocketHandlerAdapter : WebSocketHandlerAdapter(HandshakeWebSocketService()) {
  override fun handle(exchange: ServerWebExchange?, handler: Any?): Mono<HandlerResult>? {
    val webSocketHandler = handler as WebSocketHandler?
    return webSocketService.handleRequest(exchange, webSocketHandler).then(Mono.empty())
  }
}
