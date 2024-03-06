package instrumented

import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.FooQuery
import com.apollographql.apollo3.testing.mockServerTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NetworkMonitorTest {
  /**
   * A test that runs on a real device to test the network monitor.
   * Start it with Airplane mode on and the test should terminate when you disable Airplane mode.
   */
  @Test
  fun test() = mockServerTest(
      skipDelays = false,
      clientBuilder = {
        retryOnError(true)
      }
  ) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())
    mockServer.enqueueString(FooQuery.successResponse)

    val response = apolloClient.query(FooQuery()).execute()

    assertEquals(42, response.data?.foo)

    mockServer.takeRequest()
    mockServer.takeRequest()
    try {
      mockServer.takeRequest()
      fail("An exception was expected")
    } catch (_: Exception) {}
  }
}