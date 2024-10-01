package test.network

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.interceptor.RetryOnErrorInterceptor
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.assertNoRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.NetworkMonitor
import com.apollographql.apollo.network.waitForNetwork
import test.FooQuery
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.TestResult
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNull

class NetworkMonitorTest {
  @Test
  fun failFastIfOfflineTest(): TestResult {
    val fakeNetworkMonitor = FakeNetworkMonitor()

    return mockServerTest(clientBuilder = {
      retryOnErrorInterceptor(RetryOnErrorInterceptor(fakeNetworkMonitor))
      failFastIfOffline(true)
    }) {

      fakeNetworkMonitor._isOnline.value = false

      apolloClient.query(FooQuery()).toFlow()
          .test {
            awaitItem().apply {
              assertNull(data)
              assertIs<ApolloNetworkException>(exception)
              assertEquals("The device is offline", exception?.message)
            }
            mockServer.assertNoRequest()
            awaitComplete()
          }
    }
  }

  @Test
  fun networkMonitorInterceptorTest(): TestResult {
    val fakeNetworkMonitor = FakeNetworkMonitor()

    return mockServerTest(clientBuilder = {
      addInterceptor(NetworkMonitorInterceptor(fakeNetworkMonitor))
    }) {

      fakeNetworkMonitor._isOnline.value = false

      apolloClient.query(FooQuery()).toFlow()
          .test {
            // Initially the network is offline, make sure there's no request being made
            assertFails { awaitItem() }
            mockServer.assertNoRequest()

            // Enqueue response and enable network
            mockServer.enqueueString(FooQuery.successResponse)
            fakeNetworkMonitor._isOnline.value = true

            // We're expecting an item now
            assertEquals(42, awaitItem().data?.foo)
            mockServer.takeRequest()
            mockServer.assertNoRequest()

            awaitComplete()
          }
    }
  }
}

class FakeNetworkMonitor: NetworkMonitor {
  val _isOnline = MutableStateFlow(false)

  override val isOnline: StateFlow<Boolean?>
    get() = _isOnline.asStateFlow()

  override fun close() {}
}

class NetworkMonitorInterceptor(private val networkMonitor: NetworkMonitor): ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      // Wait for network before going down the chain
      networkMonitor.waitForNetwork()
      emitAll(chain.proceed(request))
    }
  }
}

class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

fun mockServerTest(
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
) = runTest() {
  MockServer().use { mockServer ->
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .apply(clientBuilder)
        .build()
        .use {apolloClient ->
          MockServerTest(mockServer, apolloClient, this).block()
        }
  }
}
