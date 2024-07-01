package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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
