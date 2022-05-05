package com.apollographql.apollo.sample.server

import com.apollographql.apollo.sample.server.KtorServerInteractor.Companion.PATH_HELLO_WORLD
import com.apollographql.apollo.sample.server.KtorServerInteractor.Companion.PAYLOAD_HELLO_WORLD
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * This test verifies that a ktor server can be started and tests can make REST requests to it.
 *
 * It is the basis of all E2E tests on gradle modules that depend on :sse-mockserver, such as :sse.
 */
class KtorServerInteractorTest {

  private var job: Job? = null

  @Before
  fun setup() {
    job = CoroutineScope(Dispatchers.IO)
        .launch {
          KtorServerInteractor()
              .invoke()
        }

    runBlocking {
      delay(300) // we need to wait for server to start
    }
  }

  @After
  fun tearDown() {
    job?.cancel()
  }

  @Test
  fun `Given ktor is running When connect Then get hello`() {

    val client = OkHttpClient.Builder()
        .build()

    val request = Request.Builder()
        .url("http://localhost:8080/$PATH_HELLO_WORLD")
        .build()

    client.newCall(request)
        .execute()
        .let {
          assertEquals(200, it.code)
          assertEquals(PAYLOAD_HELLO_WORLD, it.body?.string())
        }

  }
}