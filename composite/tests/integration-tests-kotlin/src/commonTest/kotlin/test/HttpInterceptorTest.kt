package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.LoggingInterceptor
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readResource
import kotlin.test.Test

class HttpInterceptorTest {
  @Test
  fun testLoggingInterceptor() {
    val mockServer = MockServer()
    val client = ApolloClient(
        networkTransport = HttpNetworkTransport(
            serverUrl = mockServer.url(),
            interceptors = listOf(LoggingInterceptor())
        )
    )

    mockServer.enqueue(readResource("HeroNameResponse.json"))

    runWithMainLoop {
      client.query(HeroNameQuery())
    }
  }
}