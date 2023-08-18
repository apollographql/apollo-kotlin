package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.mockserver.internal.CommonWebSocketMockServer

@ApolloInternal
actual fun WebSocketMockServer(port: Int): WebSocketMockServer = CommonWebSocketMockServer(port)
