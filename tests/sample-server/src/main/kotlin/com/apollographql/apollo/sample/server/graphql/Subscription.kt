package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.CurrentWebSocket
import com.apollographql.apollo3.annotations.GraphQLObject
import com.apollographql.apollo3.api.ExecutionContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.http4k.websocket.WsStatus

@GraphQLObject(name = "Subscription")
class SubscriptionRoot {
  fun count(to: Int, delayMillis: Int): Flow<Int> = flow {
    repeat(to) {
      emit(it)
      if (delayMillis > 0) {
        delay(delayMillis.toLong())
      }
    }
  }

  fun countString(to: Int, delayMillis: Int): Flow<String> = flow {
    repeat(to) {
      emit(it.toString())
      if (delayMillis > 0) {
        delay(delayMillis.toLong())
      }
    }
  }

  fun time(): Flow<Int> = flow {
    repeat(100) {
      emit(it)
      delay(100)
    }
  }

  fun operationError(): Flow<String> = flow<String> {
    throw Exception("Woops")
  }

  fun graphqlAccessError(after: Int): Flow<Int?> = flow {
    repeat(after) {
      emit(it)
    }

    error("Woops")
  }

  fun closeWebSocket(executionContext: ExecutionContext): Flow<String> = flow {
    executionContext[CurrentWebSocket]!!.ws.close(WsStatus(1011, "closed"))

    emit("closed")
  }
}