package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerTest
import kotlinx.coroutines.flow.toList
import multipart.CounterSubscription
import multipart.MyQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipartTest {
  @Test
  fun emptyLastPartIsIgnored() = mockServerTest {
    mockServer.enqueue(
        MockResponse.Builder()
            .addHeader("Content-Type", "multipart/mixed; boundary=\"-\"")
            .body("---\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: 31\r\n" +
                "\r\n" +
                "{\"data\":{\"__typename\":\"Query\"}}\r\n" +
                "---\r\n" +
                "-----\r\n"
            )
            .build()
    )

    apolloClient.query(MyQuery()).execute()
  }

  @Test
  fun emptyObjectLastPartIsIgnored() = runTest {
    val mockServer = MockServer()

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(HttpNetworkTransport.Builder().serverUrl(mockServer.url()).build())
        .build()

    mockServer.enqueue(
        MockResponse.Builder()
            .addHeader("Content-Type", "multipart/mixed; boundary=\"graphql\"")
            .body(
                "--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"counter\":{\"count\":42}}}}\r\n" +
                "--graphql\r\nContent-Type: application/json\r\n\r\n{}\r\n--graphql--\r\n"
            )
            .build()
    )

    val responses = apolloClient.subscription(CounterSubscription()).toFlow().toList()

    assertEquals(1, responses.size)
    assertEquals(42, responses[0].dataOrThrow().counter?.count)

    mockServer.close()
    apolloClient.close()
  }


  @Test
  fun singleEmptyObjectIsIgnored() = runTest {
    val mockServer = MockServer()

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(HttpNetworkTransport.Builder().serverUrl(mockServer.url()).build())
        .build()

    mockServer.enqueue(
        MockResponse.Builder()
            .addHeader("Content-Type", "multipart/mixed; boundary=\"graphql\"")
            .body(
              "--graphql\r\nContent-Type: application/json\r\n\r\n{}\r\n--graphql--\r\n"
            )
            .build()
    )

    val responses = apolloClient.subscription(CounterSubscription()).toFlow().toList()

    assertEquals(0, responses.size)

    mockServer.close()
    apolloClient.close()
  }
}
