@file:Suppress("DEPRECATION")

package com.apollographql.apollo.mockserver.test

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.mockserver.MockServer
import com.apollographql.apollo.mockserver.enqueueString
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SocketTest {
  @Test
  fun writeMoreThan8kToTheSocket() = runTest {
    val mockServer = MockServer()

    val builder = StringBuilder()
    0.until(10000).forEach {
      builder.append("aa")
    }

    mockServer.enqueueString(builder.toString())
    val str = builder.toString()
    val engine = DefaultHttpEngine()
    val response = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())

    assertEquals(response.body!!.readUtf8(), str)

    mockServer.close()
  }
}
