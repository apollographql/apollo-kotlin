package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.mockserver.MockResponse
import okio.ByteString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun assertMockResponse(
    mockResponse: MockResponse,
    body: ByteString,
    httpResponse: HttpResponse,
) {
  assertEquals(body, httpResponse.body!!.readByteString())
  assertEquals(mockResponse.statusCode, httpResponse.statusCode)
  // JS MockServer serves headers in lowercase, so convert before comparing
  // Also, remove 'Transfer-Encoding' header before comparison since it is changed by the Apple client
  val mockResponseHeaders = mockResponse.headers.map { it.key.lowercase() to it.value.lowercase() }.filterNot { (key, _) -> key == "transfer-encoding" }
  val httpResponseHeaders = httpResponse.headers.map { it.name.lowercase() to it.value.lowercase() }.filterNot { (key, _) -> key == "transfer-encoding" }

  // Use contains because there can be more headers than requested in the response
  assertTrue(httpResponseHeaders.containsAll(mockResponseHeaders), "Headers do not match: httpResponseHeaders=$httpResponseHeaders mockResponseHeadersLowercase=$mockResponseHeaders")
}
