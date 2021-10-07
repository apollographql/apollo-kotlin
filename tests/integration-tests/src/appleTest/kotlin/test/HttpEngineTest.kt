package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.runTest
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class HttpEngineTest {
  @Test
  fun canReadNSError() = runTest {
    val apolloClient = ApolloClient("https://inexistent.host/graphql")

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    val apolloNetworkException = result.exceptionOrNull()
    assertNotNull(apolloNetworkException)
    assertIs<ApolloNetworkException>(apolloNetworkException)

    val cause = apolloNetworkException.platformCause
    // assertIs doesn't work with Obj-C classes so we rely on `check` instead
    // assertIs<NSError>(cause)
    check(cause is NSError)

    assertEquals(NSURLErrorCannotFindHost, cause.code)
    assertEquals(NSURLErrorDomain, cause.domain)
  }
}