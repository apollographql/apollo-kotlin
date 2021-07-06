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
import com.apollographql.apollo3.network.http.canBeBatched
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okio.Buffer
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFails

class QueryBatchingTest {
  @Test
  @Ignore // because it uses a real-life server that might be down
  fun testAgainstARealServer() {
    val apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer("https://apollo-fullstack-tutorial.herokuapp.com/graphql"),
            engine = BatchingHttpEngine(),
        )
    )

    runWithMainLoop {
      val result1 = async {
        apolloClient.query(GetLaunchQuery())
      }
      val result2 = async {
        apolloClient.query(GetLaunch2Query())
      }
      assertEquals("83", result1.await().data?.launch?.id)
      assertEquals("84", result2.await().data?.launch?.id)
    }
  }

  @Test
  fun queriesAreBatchedByDefault() {
    val response = """
    [{"data":{"launch":{"id":"83"}}},{"data":{"launch":{"id":"84"}}}]
    """.trimIndent()

    val mockServer = MockServer()
    mockServer.enqueue(response)
    val apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 300
            ),
        )
    )

    runWithMainLoop {
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
      val requests = AnyAdapter.fromJson(BufferedSourceJsonReader(Buffer().write(request.body))) as List<Map<String, Any?>>
      assertEquals(2, requests.size)
      assertEquals("GetLaunch", requests[0]["operationName"])
      assertEquals("GetLaunch2", requests[1]["operationName"])

      // Only one request must have been sent
      assertFails {
        mockServer.takeRequest()
      }
    }
  }

  @Test
  fun queriesAreNotBatchedIfSubmitedFarAppart() {
    val mockServer = MockServer()
    mockServer.enqueue("""[{"data":{"launch":{"id":"83"}}}]""")
    mockServer.enqueue("""[{"data":{"launch":{"id":"84"}}}]""")
    val apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 10
            ),
        )
    )

    runWithMainLoop {
      val result1 = async {
        apolloClient.query(GetLaunchQuery())
      }
      val result2 = async {
        // Wait for the first query to be executed
        delay(50)
        apolloClient.query(GetLaunch2Query())
      }

      assertEquals("83", result1.await().data?.launch?.id)
      assertEquals("84", result2.await().data?.launch?.id)

      mockServer.takeRequest()
      mockServer.takeRequest()
    }
  }

  @Test
  fun queriesCanBeOptOutOfBatching() {
    val mockServer = MockServer()
    mockServer.enqueue("""{"data":{"launch":{"id":"83"}}}""")
    mockServer.enqueue("""[{"data":{"launch":{"id":"84"}}}]""")
    val apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = BatchingHttpEngine(
                batchIntervalMillis = 300
            ),
        )
    )

    runWithMainLoop {
      val result1 = async {
        apolloClient.query(ApolloRequest(GetLaunchQuery()).canBeBatched(false))
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
}
