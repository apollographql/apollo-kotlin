package com.apollographql.apollo3.mockserver

class QueueMockServerHandler : MockServerHandler {
  private val queue = ArrayDeque<MockResponse>()

  fun enqueue(response: MockResponse) {
    queue.add(response)
  }

  override fun handle(request: MockRecordedRequest): MockResponse {
    return queue.removeFirstOrNull() ?: error("No more responses in queue")
  }

  override fun copy(): QueueMockServerHandler {
    return QueueMockServerHandler().apply {
      queue.addAll(this@QueueMockServerHandler.queue)
    }
  }
}
