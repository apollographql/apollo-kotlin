package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.network.http.ApolloHttpLoggingInterceptor
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readResource
import kotlin.test.Test

class HttpInterceptorTest {
  @Test
  fun testLoggingInterceptor() {
    val mockServer = MockServer()
    val client = ApolloClient(
        networkTransport = ApolloHttpNetworkTransport(
            serverUrl = mockServer.url(),
            interceptors = listOf(ApolloHttpLoggingInterceptor())
        )
    )

    mockServer.enqueue(readResource("HeroNameResponse.json"))

    runWithMainLoop {
      client.query(HeroNameQuery())
    }
  }
}