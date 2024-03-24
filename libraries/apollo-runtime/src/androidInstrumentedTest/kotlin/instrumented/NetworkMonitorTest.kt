package instrumented

import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.assertNoRequest
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.network.NetworkMonitor
import com.apollographql.apollo3.testing.FooQuery
import com.apollographql.apollo3.testing.mockServerTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkMonitorTest {
  /**
   * A test that runs on a real device to test the network monitor.
   * Start it with Airplane mode on and the test should terminate when you disable Airplane mode.
   */
  @Test
  fun test() = mockServerTest(
      skipDelays = false,
      clientBuilder = {
        networkMonitor(NetworkMonitor(InstrumentationRegistry.getInstrumentation().context))
        retryOnError { true }
      }
  ) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())
    mockServer.enqueueString(FooQuery.successResponse)

    val response = apolloClient.query(FooQuery()).execute()

    assertEquals(42, response.data?.foo)

    mockServer.takeRequest()
    mockServer.takeRequest()
    mockServer.assertNoRequest()
  }
}