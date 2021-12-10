package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.server.spring.subscriptions.ApolloSubscriptionHooks
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketSession

class MyApolloSubscriptionHooks : ApolloSubscriptionHooks {
  override fun onConnectWithContext(connectionParams: Map<String, String>,
                                    session: WebSocketSession,
                                    graphQLContext: Map<*, Any>?): Map<*, Any>? {
    val returns = connectionParams["return"]

    if (returns != null) {
      when  {
        returns == "success"-> Unit
        returns == "error" -> throw IllegalStateException("Error")
        returns.startsWith("close") -> {
          val code = Regex("close\\(([0-9]*)\\)").matchEntire(`returns`)
              ?.let { it.groupValues[1].toIntOrNull() }
              ?: 1001

          session.close(CloseStatus(code)).block()
        }
      }
    }

    return super.onConnectWithContext(connectionParams, session, graphQLContext)
  }
}
