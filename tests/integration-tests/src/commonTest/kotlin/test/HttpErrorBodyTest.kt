package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ApolloExperimental
class HttpErrorBodyTest {
  @Test
  fun canReadErrorBody() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpExposeErrorBody(true)
        .build()

    mockServer.enqueueString(
        statusCode = 500,
        string = "Ooops"
    )

    val e = apolloClient.query(HeroNameQuery()).execute().exception as ApolloHttpException
    assertEquals("Ooops", e.body?.readUtf8())

    apolloClient.close()
    mockServer.close()
  }
}
