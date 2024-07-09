package com.apollographql.apollo.debugserver

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.debugserver.internal.server.Server
import com.apollographql.apollo.debugserver.internal.server.createServer
import java.util.concurrent.atomic.AtomicReference

object ApolloDebugServer {
  private val apolloClients = AtomicReference<Map<ApolloClient, String>>(emptyMap())
  private var server: Server? = null

  fun registerApolloClient(apolloClient: ApolloClient, id: String = "client") {
    if (apolloClients.get().containsKey(apolloClient)) error("Client '$apolloClient' already registered")
    if (apolloClients.get().containsValue(id)) error("Name '$id' already registered")
    apolloClients.set(apolloClients.get() + (apolloClient to id))
    startOrStopServer()
  }

  fun unregisterApolloClient(apolloClient: ApolloClient) {
    apolloClients.set(apolloClients.get() - apolloClient)
    startOrStopServer()
  }

  private fun startOrStopServer() {
    if (apolloClients.get().isEmpty()) {
      server?.stop()
      server = null
    } else {
      if (server == null) {
        server = createServer(apolloClients).apply {
          start()
        }
      }
    }
  }
}
