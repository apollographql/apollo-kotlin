package com.apollographql.apollo3.engine.tests

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine

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