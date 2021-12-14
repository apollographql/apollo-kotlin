package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.server.spring.GraphQLConfigurationProperties
import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionHooks
import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionProtocolHandler
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@SpringBootApplication
class DefaultApplication {

}


@Configuration
class MyConfiguration {
  @Bean
  fun apolloSubscriptionHooks(): ApolloSubscriptionHooks = MyApolloSubscriptionHooks()

  @Bean
  fun myWebSocketHandler(
      handler: ApolloSubscriptionProtocolHandler,
      objectMapper: ObjectMapper,
  ): MyWebSocketHandler = MyWebSocketHandler(handler, objectMapper)

  @Bean
  fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = MyWebSocketHandlerAdapter()

  @Bean
  fun myHandlerMapping(config: GraphQLConfigurationProperties, myWebSocketHandler: MyWebSocketHandler): HandlerMapping =
      SimpleUrlHandlerMapping(mapOf("subscriptions" to myWebSocketHandler), 0)
}
