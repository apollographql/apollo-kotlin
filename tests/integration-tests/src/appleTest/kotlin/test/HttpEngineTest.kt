package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.runWithMainLoop
import platform.CFNetwork.kCFErrorHTTPSProxyConnectionFailure
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpEngineTest {
  @Test
  fun canReadNSError() = runWithMainLoop {
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

    assertTrue(when(cause.code) {
      NSURLErrorCannotFindHost -> true
      kCFErrorHTTPSProxyConnectionFailure.toLong() -> true // Happens locally if a proxy is running
      else -> false
    })
    assertEquals(NSURLErrorDomain, cause.domain)
  }
}