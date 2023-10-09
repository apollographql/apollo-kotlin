package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo3.annotations.ApolloObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ApolloObject
class Subscription {
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
}