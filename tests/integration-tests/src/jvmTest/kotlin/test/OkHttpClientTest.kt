package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.okHttpClient
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
      mockServer.enqueueError(statusCode = 200)

      val apolloClient = ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .okHttpClient(okHttpClient)
          .build()

      kotlin.runCatching {
        apolloClient.query(HeroNameQuery()).execute()
      }

      assertEquals("Test-Value", mockServer.awaitRequest().headers["Test-Header"])
    }
  }
}
