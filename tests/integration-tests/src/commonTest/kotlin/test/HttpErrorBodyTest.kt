package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@ApolloExperimental
class HttpErrorBodyTest {
  @Test
  fun canReadErrorBody() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpExposeErrorBody(true)
        .build()

    mockServer.enqueue(
            statusCode = 500,
        string = "Ooops"
    )

    try {
      apolloClient.query(HeroNameQuery()).execute()
      fail("An exception was expected")
    } catch (e: ApolloHttpException) {
      assertEquals("Ooops", e.body?.readUtf8())
    }

    apolloClient.dispose()
    mockServer.stop()
  }
}
