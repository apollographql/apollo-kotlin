package com.apollographql.apollo.sample.server

import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration


@Component
class RootQuery : Query {
  fun random(): Int = 42
  fun time(): Int = 0
}

@Component
class RootSubscription : Subscription {
  fun count(to: Int, delayMillis: Int) = flow {
    repeat(to) {
      emit(it)
      if (delayMillis > 0) {
        delay(delayMillis.toLong())
      }
    }
  }.asPublisher()

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
}