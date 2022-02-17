package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.okHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Test
import kotlin.test.assertEquals

class OkHttpClientTest {
  @Test
  fun okHttpInterceptors() {
    val interceptor = Interceptor {
      it.proceed(it.request().newBuilder().header("Test-Header", "Test-Value").build())
    }
    val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()


    runBlocking {
      val mockServer = MockServer()
      mockServer.enqueue(MockResponse())

      val apolloClient = ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .okHttpClient(okHttpClient)
          .build()

      kotlin.runCatching {
        apolloClient.query(HeroNameQuery()).execute()
      }

      assertEquals("Test-Value", mockServer.takeRequest().headers["Test-Header"])
    }
  }
}
