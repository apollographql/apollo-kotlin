package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import platform.Foundation.NSURLSessionConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultHttpEngineTest {
  @Test
  fun sessionConfigurationIsUsed() = runTest {
    val engine = DefaultHttpEngine(
        nsUrlSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
          HTTPAdditionalHeaders = mapOf("header" to "value")
        }
    )

    val mockServer = MockServer()
    mockServer.enqueueString("response")
    engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
    assertEquals("value", mockServer.takeRequest().headers["header"])
  }
}
