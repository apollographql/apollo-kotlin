package instrumented

import androidx.test.platform.app.InstrumentationRegistry
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.mockserver.assertNoRequest
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.network.NetworkMonitor
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.testing.FooQuery
import com.apollographql.apollo3.testing.mockServerTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkMonitorTest {
  /**
   * A test that runs on a real device to test the network monitor.
   * Start it with Airplane mode on and the test should terminate when you disable Airplane mode.
   */
  class FaultyHttpEngine: HttpEngine {
    private var first = true
    var received = 0
    val delegate = DefaultHttpEngine()
    override suspend fun execute(request: HttpRequest): HttpResponse {
      received++
      if (first) {
        first = false
        throw ApolloNetworkException("Ooopsie")
      } else {
        return delegate.execute(request)
      }
    }
  }

  @Test
  fun test() = mockServerTest(
      skipDelays = false,
      clientBuilder = {
        networkMonitor(NetworkMonitor(InstrumentationRegistry.getInstrumentation().context))
        retryOnError { true }
        httpEngine(FaultyHttpEngine())
      }
  ) {
    mockServer.enqueueString(FooQuery.successResponse)

    val response = apolloClient.query(FooQuery()).execute()

    assertEquals(42, response.data?.foo)

    mockServer.takeRequest()
    mockServer.assertNoRequest()
  }
}