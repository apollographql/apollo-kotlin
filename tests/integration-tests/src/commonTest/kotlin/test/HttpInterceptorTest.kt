package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.LoggingInterceptor
import com.apollographql.apollo3.testing.runTest
import testFixtureToUtf8
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class HttpInterceptorTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun testLoggingInterceptor() = runTest(before = { setUp() }, after = { tearDown() }) {
    val client = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .serverUrl(
                    mockServer.url(),
                ).interceptors(
                    listOf(LoggingInterceptor())
                ).build()
        )
        .build()

    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    client.query(HeroNameQuery()).execute()
  }
}