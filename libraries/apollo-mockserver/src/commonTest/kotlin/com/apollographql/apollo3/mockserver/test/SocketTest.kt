package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SocketTest {
  @Test
  fun writeMoreThan8kToTheSocket() = runTest {
    val mockServer = MockServer()

    val builder = StringBuilder()
    0.until(10000).forEach {
      builder.append(Random.nextInt())
    }

    mockServer.enqueue(builder.toString())
    val str = builder.toString()
    val engine = DefaultHttpEngine()
    val response = engine.execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())

    assertEquals(response.body!!.readUtf8(), str)

    mockServer.stop()
  }
}
