package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.toList
import multipart.CounterSubscription
import kotlin.test.Test
import kotlin.test.assertContentEquals

class MultipartSubscriptionsTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    val serverUrl = mockServer.url()
    apolloClient = ApolloClient.Builder()
        .serverUrl(serverUrl)
        .subscriptionNetworkTransport(HttpNetworkTransport.Builder().serverUrl(serverUrl).build())
        .build()
  }

  private suspend fun tearDown() {
    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun multipartSubscriptions() = runTest(before = { setUp() }, after = { tearDown() }) {
    val parts = listOf(
        """{"data":{"counter":{"count":0}}}""",
        """{"incremental": [{"data":{"counter":{"count":1}},"path":[]}],"hasNext":true}""",
        """{"incremental": [{"data":{"counter":{"count":2}},"path":[]}],"hasNext":true}""",
        """{"incremental": [{"data":{"counter":{"count":3}},"path":[]}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(parts, chunksDelayMillis = 100)

    val expectedDataList = listOf(
        CounterSubscription.Data(CounterSubscription.Counter(0)),
        CounterSubscription.Data(CounterSubscription.Counter(1)),
        CounterSubscription.Data(CounterSubscription.Counter(2)),
        CounterSubscription.Data(CounterSubscription.Counter(3)),
    )

    val actualDataList = apolloClient.subscription(CounterSubscription()).toFlow().toList().map { it.data!! }

    assertContentEquals(expectedDataList, actualDataList)
  }
}
