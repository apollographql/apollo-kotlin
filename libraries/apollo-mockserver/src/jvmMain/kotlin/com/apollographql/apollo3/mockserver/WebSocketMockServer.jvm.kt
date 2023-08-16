package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.mockserver.internal.CommonWebSocketMockServer

actual fun WebSocketMockServer(port: Int): WebSocketMockServer = CommonWebSocketMockServer(port)
