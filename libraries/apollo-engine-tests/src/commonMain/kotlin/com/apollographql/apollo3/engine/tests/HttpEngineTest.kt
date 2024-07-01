package com.apollographql.apollo.engine.tests

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.http.ByteStringHttpBody
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import okio.use
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


@ApolloInternal
suspend fun errorWithBody(engine: (Long) -> HttpEngine) = MockServer().use { mockServer ->
  mockServer.enqueueString(
      statusCode = 500,
      string = "Ooops"
  )
  val httpResponse = engine(60_000).execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
  assertEquals(500, httpResponse.statusCode)
  assertEquals("Ooops", httpResponse.body?.readUtf8())
}

@ApolloInternal
suspend fun headers(engine: (Long) -> HttpEngine) = MockServer().use { mockServer ->
  mockServer.enqueue(
      MockResponse.Builder()
          .statusCode(204)
          .addHeader("responseHeader1", "responseValue1")
          .addHeader("responseHeader2", "responseValue2")
          .build()
  )
  val httpResponse = engine(60_000).execute(
      HttpRequest.Builder(HttpMethod.Get, mockServer.url())
          .addHeader("requestHeader1", "requestValue1")
          .addHeader("requestHeader2", "requestValue2")
          .build()
  )
  val request = mockServer.awaitRequest()
  assertEquals("requestValue1", request.headers["requestHeader1"])
  assertEquals("requestValue2", request.headers["requestHeader2"])

  assertEquals("responseValue1", httpResponse.headers.valueOf("responseHeader1"))
  assertEquals("responseValue2", httpResponse.headers.valueOf("responseHeader2"))
}

@ApolloInternal
suspend fun post(engine: (Long) -> HttpEngine) = MockServer().use { mockServer ->
  mockServer.enqueue(
      MockResponse.Builder()
          .statusCode(204)
          .build()
  )
  engine(60_000).execute(
      HttpRequest.Builder(HttpMethod.Post, mockServer.url())
          .body(ByteStringHttpBody("text/plain", "body"))
          .build()
  )
  val request = mockServer.awaitRequest()
  assertEquals("POST", request.method)
  assertEquals("body", request.body.utf8())
  // With ktor we get "text/plain; charset=UTF-8"
  assertTrue(request.headers["Content-Type"]!!.startsWith("text/plain"))
  assertEquals("body".length.toString(), request.headers["Content-Length"])
}

@ApolloInternal
suspend fun connectTimeout(engine: (Long) -> HttpEngine) = MockServer().use { mockServer ->
  assertFailsWith<ApolloException> {

    // Not routable IP, results in a timeout (see https://stackoverflow.com/a/904609/15695)
    engine(500).execute(HttpRequest.Builder(HttpMethod.Get, "http://10.0.0.0").build())
  }
}

@ApolloInternal
suspend fun readTimeout(engine: (Long) -> HttpEngine) = MockServer().use { mockServer ->
  mockServer.enqueue(
      MockResponse.Builder()
          .delayMillis(1000)
          .statusCode(200)
          .body("body")
          .build()
  )
  assertFailsWith<ApolloException> {
    engine(500).execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
  }
}

