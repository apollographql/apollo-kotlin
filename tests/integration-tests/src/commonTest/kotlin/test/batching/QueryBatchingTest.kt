package test.batching

import batching.GetLaunch2Query
import batching.GetLaunchQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ExecutionOptions.Companion.CAN_BE_BATCHED
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import okio.Buffer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QueryBatchingTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
    // This is important. JS will hang if the BatchingHttpInterceptor scope is not cancelled
    apolloClient.close()
  }

  @Test
  @Ignore // because it uses a real-life server that might be down
  fun testAgainstARealServer() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .serverUrl("https://apollo-fullstack-tutorial.herokuapp.com/graphql")
        .httpBatching()
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery()).execute()
    }
    val result2 = async {
      apolloClient.query(GetLaunch2Query()).execute()
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
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 1000)
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery()).execute()
    }
    val result2 = async {
      // Make sure GetLaunch2Query gets executed after GetLaunchQuery as there is no guarantee otherwise
      // 300ms batchIntervalMillis and 50ms delay here should be enough. Increase values if some tests become flaky
      delay(50)
      apolloClient.query(GetLaunch2Query()).execute()
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    val request = mockServer.takeRequest()
    val requests = AnyApolloAdapter.fromJson(Buffer().write(request.body).jsonReader(), ScalarAdapters.Empty)

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
  fun queriesAreNotBatchedIfSubmittedFarApart() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("""[{"data":{"launch":{"id":"83"}}}]""")
    mockServer.enqueue("""[{"data":{"launch":{"id":"84"}}}]""")
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 10)
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery()).execute()
    }
    val result2 = async {
      // Wait for the first query to be executed
      delay(200)
      apolloClient.query(GetLaunch2Query()).execute()
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
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 1000)
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
          .canBeBatched(false)
          // Override headers, make sure this doesn't change canBeBatched
          .httpHeaders(listOf(HttpHeader("client0", "0")))
          .execute()
    }
    val result2 = async {
      // Make sure GetLaunch2Query gets executed after GetLaunchQuery as there is no guarantee otherwise
      delay(50)
      apolloClient.query(GetLaunch2Query()).execute()
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    mockServer.takeRequest()
    mockServer.takeRequest()
  }

  @Test
  fun queriesCanBeOptInOfBatching() = runTest(before = { setUp() }, after = { tearDown() }) {
    val response = """
    [{"data":{"launch":{"id":"83"}}},{"data":{"launch":{"id":"84"}}}]
    """.trimIndent()

    mockServer.enqueue(response)
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 1000)
        // Opt out by default
        .canBeBatched(false)
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
          // Opt in this one
          .canBeBatched(true)
          .execute()
    }
    val result2 = async {
      // Make sure GetLaunch2Query gets executed after GetLaunchQuery as there is no guarantee otherwise
      // 300ms batchIntervalMillis and 50ms delay here should be enough. Increase values if some tests become flaky
      delay(50)
      apolloClient.query(GetLaunch2Query())
          // Opt in this one
          .canBeBatched(true)
          .execute()
    }

    assertEquals("83", result1.await().data?.launch?.id)
    assertEquals("84", result2.await().data?.launch?.id)

    val request = mockServer.takeRequest()
    val requests = AnyApolloAdapter.fromJson(Buffer().write(request.body).jsonReader(), ScalarAdapters.Empty)

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
  fun httpHeadersOnClientAreKept() = runTest(before = { setUp() }, after = { tearDown() }) {
    val response = """
    [{"data":{"launch":{"id":"83"}}},{"data":{"launch":{"id":"84"}}}]
    """.trimIndent()

    mockServer.enqueue(response)
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 1000)
        .httpHeaders(
            listOf(
                HttpHeader("client0", "0"),
                HttpHeader("client1", "1")
            )
        )
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery()).execute()
    }
    val result2 = async {
      delay(50)
      apolloClient.query(GetLaunch2Query()).execute()
    }
    result1.await()
    result2.await()
    val request = mockServer.takeRequest()
    // Only one request must have been sent
    assertFails {
      mockServer.takeRequest()
    }
    assertTrue(request.headers["client0"] == "0")
    assertTrue(request.headers["client1"] == "1")
    assertFalse(request.headers.keys.contains(CAN_BE_BATCHED))
  }

  @Test
  fun commonHttpHeadersOnRequestsAreKept() = runTest(before = { setUp() }, after = { tearDown() }) {
    val response = """
    [{"data":{"launch":{"id":"83"}}},{"data":{"launch":{"id":"84"}}}]
    """.trimIndent()

    mockServer.enqueue(response)
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpBatching(batchIntervalMillis = 1000)
        .build()

    val result1 = async {
      apolloClient.query(GetLaunchQuery())
          .canBeBatched(true)
          .addHttpHeader("query1-only", "0")
          .addHttpHeader("query1+query2-same-value", "0")
          .addHttpHeader("query1+query2-different-value", "0")
          .execute()
    }
    val result2 = async {
      delay(50)
      apolloClient.query(GetLaunch2Query())
          .canBeBatched(true)
          .addHttpHeader("query2-only", "0")
          .addHttpHeader("query1+query2-same-value", "0")
          .addHttpHeader("query1+query2-different-value", "1")
          .execute()
    }
    result1.await()
    result2.await()
    val request = mockServer.takeRequest()
    assertTrue(request.headers["query1+query2-same-value"] == "0")
    assertFalse(request.headers.keys.contains("query1-only"))
    assertFalse(request.headers.keys.contains("query2-only"))
    assertFalse(request.headers.keys.contains("query1+query2-different-value"))
    assertFalse(request.headers.keys.contains(CAN_BE_BATCHED))
  }
}
