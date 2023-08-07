package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.KtorHttpEngine
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.network.ws.KtorWebSocketEngine
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ApolloExperimental
class HttpErrorBodyTest {
  @Test
  fun defaultEngineCanReadErrorBody() {
    canReadErrorBody {
      httpEngine(DefaultHttpEngine())
      webSocketEngine(DefaultWebSocketEngine())
    }
  }

  @Test
  fun ktorEngineCanReadErrorBody() {
    canReadErrorBody {
      httpEngine(KtorHttpEngine())
      webSocketEngine(KtorWebSocketEngine())
    }
  }

  private fun canReadErrorBody(builder: ApolloClient.Builder.() -> Unit) = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpExposeErrorBody(true)
        .apply(builder)
        .build()

    mockServer.enqueue(
            statusCode = 500,
        string = "Ooops"
    )

      val e = apolloClient.query(HeroNameQuery()).execute().exception as ApolloHttpException
      assertEquals("Ooops", e.body?.readUtf8())

    apolloClient.close()
    mockServer.stop()
  }
}
