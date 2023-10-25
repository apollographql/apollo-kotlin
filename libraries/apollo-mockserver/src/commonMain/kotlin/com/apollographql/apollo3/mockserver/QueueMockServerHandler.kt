package com.apollographql.apollo3.mockserver

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock


internal class QueueMockServerHandler : MockServerHandler {

  private val lock = reentrantLock()
  private val queue = ArrayDeque<MockResponse>()

  fun enqueue(response: MockResponse) {
    lock.withLock {
      queue.add(response)
    }
  }

  override fun handle(request: MockRequestBase): MockResponse {
    var response = lock.withLock {
      queue.removeFirstOrNull() ?: error("No more responses in queue")
    }

    if (request is WebsocketMockRequest) {
      response = response.replaceWebSocketHeaders(request)
    }

    return response
  }
}
