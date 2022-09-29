package com.apollographql.apollo3.mockserver

internal expect class QueueMockServerHandler() : MockServerHandler {
  fun enqueue(response: MockResponse)

  override fun handle(request: MockRequest): MockResponse
}

internal class CommonQueueMockServerHandler : MockServerHandler {
  private val queue = ArrayDeque<MockResponse>()

  fun enqueue(response: MockResponse) {
    queue.add(response)
  }

  override fun handle(request: MockRequest): MockResponse {
    return queue.removeFirstOrNull() ?: error("No more responses in queue")
  }
}
