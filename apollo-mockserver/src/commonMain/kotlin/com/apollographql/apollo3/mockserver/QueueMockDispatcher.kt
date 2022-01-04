package com.apollographql.apollo3.mockserver

class QueueMockDispatcher : MockDispatcher {
  private val queue = ArrayDeque<MockResponse>()

  fun enqueue(response: MockResponse) {
    queue.add(response)
  }

  override fun dispatch(request: MockRecordedRequest): MockResponse {
    return queue.removeFirstOrNull() ?: error("No more responses in queue")
  }

  override fun copy(): QueueMockDispatcher {
    return QueueMockDispatcher().apply {
      queue.addAll(this@QueueMockDispatcher.queue)
    }
  }
}
