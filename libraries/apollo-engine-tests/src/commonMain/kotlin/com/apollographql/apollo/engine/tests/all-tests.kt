package com.apollographql.apollo.engine.tests

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.network.ws.WebSocketEngine

@ApolloInternal
suspend fun runAllTests(engine: (Long) -> HttpEngine, webSocketEngine: () -> WebSocketEngine, checkCloseFrame: Boolean) {
  gzipTest(engine(60_000))
  errorWithBody(engine)
  headers(engine)
  post(engine)
  connectTimeout(engine)
  readTimeout(engine)

  textFrames(webSocketEngine)
  binaryFrames(webSocketEngine)
  serverCloseNicely(webSocketEngine, checkCloseFrame)
  serverCloseAbruptly(webSocketEngine)
  headers(webSocketEngine)
}