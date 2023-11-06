package com.apollographql.apollo3.debugserver.internal.server

import com.apollographql.apollo3.ApolloClient
import java.util.concurrent.atomic.AtomicReference

internal actual fun createServer(apolloClients: AtomicReference<Map<ApolloClient, String>>): Server = NoOpServer()
