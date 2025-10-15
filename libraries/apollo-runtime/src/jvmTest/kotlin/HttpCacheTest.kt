import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.network.http.isFromHttpCache
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import test.FooMutation
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpCacheTest {
  private val successResponse = MockResponse.Builder()
      .body(FooQuery.successResponse)
      .addHeader("cache-control", "max-age=100")
      .build()

  private fun apolloClient(url: String): ApolloClient {
    return ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .httpRequestComposer(DefaultHttpRequestComposer(serverUrl = url, enablePostCaching = true))
                .httpEngine(
                    DefaultHttpEngine {
                      OkHttpClient.Builder()
                          .cache(Cache(FakeFileSystem(), "/cache".toPath(), Long.MAX_VALUE))
                          .build()
                    }
                )
                .build()
        )
        .build()
  }

  @Test
  fun queryIsCached() = runTest {
    MockServer().use { mockServer ->
      apolloClient(mockServer.url())
          .use { apolloClient ->
            mockServer.enqueue(successResponse)
            val response1 = apolloClient.query(FooQuery()).execute()
            assertEquals(42, response1.data?.foo)
            assertEquals(false, response1.isFromHttpCache)
            val response2 = apolloClient.query(FooQuery()).execute()
            assertEquals(42, response2.data?.foo)
            assertEquals(true, response2.isFromHttpCache)
          }
    }
  }

  @Test
  fun noStore() = runTest {
    MockServer().use { mockServer ->
      apolloClient(mockServer.url())
          .use { apolloClient ->
            mockServer.enqueue(successResponse)
            mockServer.enqueue(successResponse)
            val response1 = apolloClient.query(FooQuery())
                .addHttpHeader("cache-control", "no-store")
                .execute()
            assertEquals(42, response1.data?.foo)
            assertEquals(false, response1.isFromHttpCache)
            val response2 = apolloClient.query(FooQuery()).execute()
            assertEquals(42, response2.data?.foo)
            assertEquals(false, response2.isFromHttpCache)
          }
    }
  }


  @Test
  fun mutationIsNotCached() = runTest {
    MockServer().use { mockServer ->
      apolloClient(mockServer.url())
          .use { apolloClient ->
            mockServer.enqueue(successResponse)
            mockServer.enqueue(successResponse)
            val response1 = apolloClient.mutation(FooMutation()).execute()
            assertEquals(42, response1.data?.foo)
            assertEquals(false, response1.isFromHttpCache)
            val response2 = apolloClient.mutation(FooMutation()).execute()
            assertEquals(42, response2.data?.foo)
            assertEquals(false, response2.isFromHttpCache)
          }
    }
  }
}