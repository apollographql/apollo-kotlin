package test.network

import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.mockserver.assertNoRequest
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.network.NetworkMonitor
import com.apollographql.apollo3.testing.FooQuery
import com.apollographql.apollo3.testing.internal.ApolloTestResult
import com.apollographql.apollo3.testing.mockServerTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class NetworkMonitorInterceptorTest {
  @Test
  fun networkMonitorInterceptorTest(): ApolloTestResult {
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

  override fun close() {}

  override val isOnline: Boolean
    get() = _isOnline.value

  override suspend fun waitForNetwork() {
    _isOnline.takeWhile { !it }.collect()
  }
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

