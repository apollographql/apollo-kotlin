package com.apollographql.apollo3.debugserver

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.debugserver.internal.server.Server
import com.apollographql.apollo3.debugserver.internal.server.createServer

object ApolloDebugServer {
  private val apolloClients = mutableMapOf<ApolloClient, String>()
  private var server: Server? = null

  fun registerApolloClient(apolloClient: ApolloClient, id: String = "client") {
    if (apolloClients.containsKey(apolloClient)) error("Client '$apolloClient' already registered")
    if (apolloClients.containsValue(id)) error("Name '$id' already registered")
    apolloClients[apolloClient] = id
    startOrStopServer()
  }

  fun unregisterApolloClient(apolloClient: ApolloClient) {
    apolloClients.remove(apolloClient)
    startOrStopServer()
  }

  private fun startOrStopServer() {
    if (apolloClients.isEmpty()) {
      server?.stop()
    } else {
      if (server == null) {
        server = createServer(apolloClients).apply {
          start()
        }
      }
    }
  }
}
