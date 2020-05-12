package com.apollographql.apollo.network.mock

import com.apollographql.apollo.network.DataTaskCompletionHandler
import platform.Foundation.NSURLSessionDataTask

class MockSessionDataTask(
    private val completionHandler: DataTaskCompletionHandler,
    private val mockResponse: MockHttpResponse
) : NSURLSessionDataTask() {

  override fun resume() {
    with(mockResponse) {
      completionHandler(httpData, httpResponse, error)
    }
  }
}
