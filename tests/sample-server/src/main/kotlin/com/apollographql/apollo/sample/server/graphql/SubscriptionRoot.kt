package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.CurrentWebSocket
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.execution.annotation.GraphQLSubscription
import com.apollographql.execution.websocket.subscriptionId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.http4k.websocket.WsStatus

@GraphQLSubscription
class SubscriptionRoot(private val tag: String) {
  fun count(to: Int, intervalMillis: Int): Flow<Int> = flow {
    repeat(to) {
      emit(it)
      if (intervalMillis > 0) {
        delay(intervalMillis.toLong())
      }
    }
  }

  fun countString(to: Int, intervalMillis: Int): Flow<String> = flow {
    repeat(to) {
      emit(it.toString())
      if (intervalMillis > 0) {
        delay(intervalMillis.toLong())
      }
    }
  }

  fun secondsSinceEpoch(intervalMillis: Int): Flow<Double> = flow {
    while (true) {
      emit(System.currentTimeMillis().div(1000).toDouble())
      delay(intervalMillis.toLong())
    }
  }

  fun operationError(): Flow<String> = flow {
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

  fun state(executionContext: ExecutionContext, intervalMillis: Int): Flow<State> = flow {
    while (true) {
      emit(State(tag, executionContext.subscriptionId()))
      delay(intervalMillis.toLong())
    }
  }

  fun valueSharedWithSubscriptions(): Flow<Int> = flow {
    repeat(10) {
      emit(it)
      delay(100)
    }
  }
}

class State(
    val tag: String,
    val subscriptionId: String
)