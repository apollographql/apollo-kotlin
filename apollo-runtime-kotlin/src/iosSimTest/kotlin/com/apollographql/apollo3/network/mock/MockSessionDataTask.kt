package com.apollographql.apollo3.network.mock

import com.apollographql.apollo3.network.http.UrlSessionDataTaskCompletionHandler
import platform.Foundation.NSURLSessionDataTask

class MockSessionDataTask(
    private val completionHandler: UrlSessionDataTaskCompletionHandler,
    private val mockResponse: MockHttpResponse
) : NSURLSessionDataTask() {

  override fun resume() {
    with(mockResponse) {
      completionHandler(httpData, httpResponse, error)
    }
  }
}
