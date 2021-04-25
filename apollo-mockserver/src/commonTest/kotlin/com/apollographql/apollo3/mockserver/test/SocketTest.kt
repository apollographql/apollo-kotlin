package com.apollographql.apollo3.mockserver.test

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpMethod
import com.apollographql.apollo3.network.http.HttpRequest
import com.apollographql.apollo3.testing.runWithMainLoop
import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class SocketTest {
  val mockServer = MockServer()

  @Test
  fun writeMoreThan8kToTheSocket() = runWithMainLoop {
    val builder = StringBuilder()
    0.until(10000).forEach {
      builder.append(Random.nextInt())
    }

    mockServer.enqueue(MockResponse(body = builder.toString().encodeUtf8()))
    val str = builder.toString()
    val engine = DefaultHttpEngine()
    engine.execute(HttpRequest(mockServer.url(), emptyMap(), HttpMethod.Get, null)) {
      assertEquals(it.body!!.readUtf8(), str)
    }
  }
}