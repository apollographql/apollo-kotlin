package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionHooks
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class DefaultApplication {
//  @Bean
//  fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = MyWebSocketHandlerAdapter()
  @Bean
  fun apolloSubscriptionHooks(): ApolloSubscriptionHooks = MyApolloSubscriptionHooks()
}



