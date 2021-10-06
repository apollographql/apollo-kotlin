package test.batching

import batching.GetLaunch2Query
import batching.GetLaunchQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.BatchingHttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.withCanBeBatched
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okio.Buffer
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class QueryBatchingTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

      private suspend fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
    // This is important. JS will hang if the BatchingHttpEngine scope is not cancelled
    apolloClient.dispose()
  }

  @Test
  @Ignore // because it uses a real-life server that might be down
  fun testAgainstARealServer() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            serverUrl = "https://apollo-fullstack-tutorial.herokuapp.com/graphql",
            engine = BatchingHttpEngine(),
        )
    )

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
    }
    val result2 = async {
      apolloClient.query(GetLaunch2Query())
    }
    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)
  }

  @Test
  fun queriesAreBatchedByDefault() = runTest(before = { setUp() }, after = { tearDown() }) {
    val response = """
    [{"data":{"launch":{"id":"83"}}},{"data":{"launch":{"id":"84"}}}]
    """.trimIndent()

    mockServer.enqueue(response)
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 300
            ),
        )
    )

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
    }
    val result2 = async {
      // Make sure GetLaunch2Query gets executed after GetLaunchQuery as there is no guarantee otherwise
      // 300ms batchIntervalMillis and 50ms delay here should be enough. Increase values if some tests become flaky
      delay(50)
      apolloClient.query(GetLaunch2Query())
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    val request = mockServer.takeRequest()
    val requests = AnyAdapter.fromJson(BufferedSourceJsonReader(Buffer().write(request.body)))

    assertIs<List<Map<String, Any?>>>(requests)
    assertEquals(2, requests.size)
    assertEquals("GetLaunch", requests[0]["operationName"])
    assertEquals("GetLaunch2", requests[1]["operationName"])

    // Only one request must have been sent
    assertFails {
      mockServer.takeRequest()
    }
  }

  @Test
  fun queriesAreNotBatchedIfSubmitedFarAppart() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("""[{"data":{"launch":{"id":"83"}}}]""")
    mockServer.enqueue("""[{"data":{"launch":{"id":"84"}}}]""")
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 10
            ),
        )
    )

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
    }
    val result2 = async {
      // Wait for the first query to be executed
      delay(200)
      apolloClient.query(GetLaunch2Query())
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    mockServer.takeRequest()
    mockServer.takeRequest()
  }

  @Test
  fun queriesCanBeOptOutOfBatching() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("""{"data":{"launch":{"id":"83"}}}""")
    mockServer.enqueue("""[{"data":{"launch":{"id":"84"}}}]""")
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 300
            ),
        )
    )

    val result1 = async {
      apolloClient.query(ApolloRequest(GetLaunchQuery()).withCanBeBatched(false))
    }
    val result2 = async {
      // Make sure GetLaunch2Query gets executed after GetLaunchQuery as there is no guarantee otherwise
      delay(50)
      apolloClient.query(GetLaunch2Query())
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    mockServer.takeRequest()
    mockServer.takeRequest()
  }
}
