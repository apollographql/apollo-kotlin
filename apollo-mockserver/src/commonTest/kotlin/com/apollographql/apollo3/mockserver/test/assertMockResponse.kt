package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.mockserver.MockResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun assertMockResponse(
    mockResponse: MockResponse,
    httpResponse: HttpResponse,
) {
  assertEquals(mockResponse.body.utf8(), httpResponse.body!!.readUtf8())
  assertEquals(mockResponse.statusCode, httpResponse.statusCode)
  // JS MockServer serves headers in lowercase, so convert before comparing
  val mockResponseHeaders = mockResponse.headers.map { it.key.lowercase() to it.value.lowercase() }
  val httpResponseHeaders = httpResponse.headers.map { it.name.lowercase() to it.value.lowercase() }

  // Use contains because there can be more headers than requested in the response
  assertTrue(httpResponseHeaders.containsAll(mockResponseHeaders), "Headers do not match: httpResponseHeaders=$httpResponseHeaders mockResponseHeadersLowercase=$mockResponseHeaders")
}
