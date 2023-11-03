package com.apollographql.apollo3.debugserver.internal.server

import com.apollographql.apollo3.ApolloClient

internal actual fun createServer(apolloClients: Map<ApolloClient, String>): Server = NoOpServer()

