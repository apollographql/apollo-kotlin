package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asPublisher
import org.springframework.stereotype.Component

@Component
class RootSubscription : Subscription {
  @GraphQLDescription("Count from 0 until 'to', waiting 'delayMillis' after each response")
  fun count(to: Int, delayMillis: Int) = flow {
    repeat(to) {
      emit(it)
      if (delayMillis > 0) {
        delay(delayMillis.toLong())
      }
    }
  }.asPublisher()

  @GraphQLDescription("Count from 0 until 'to', waiting 'delayMillis' after each response and returns each result as a String")
  fun countString(to: Int, delayMillis: Int) = flow {
    repeat(to) {
      emit(it.toString())
      if (delayMillis > 0) {
        delay(delayMillis.toLong())
      }
    }
  }.asPublisher()

  fun time() = flow {
    repeat(100) {
      emit(it)
      delay(100)
    }
  }.asPublisher()

  @GraphQLDescription("Trigger an error when accessed")
  fun operationError() = flow<String> {
    throw Exception("Woops")
  }.asPublisher()
}