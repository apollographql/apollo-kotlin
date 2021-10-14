package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.runTest
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.cinterop.pointed
import platform.CFNetwork.kCFErrorDomainCFNetwork
import platform.CFNetwork.kCFErrorHTTPSProxyConnectionFailure
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDomain
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    assertTrue(
        when {
          // Happens locally if a proxy is running
          cause.domain == (CFBridgingRelease(kCFErrorDomainCFNetwork) as String) && cause.code == kCFErrorHTTPSProxyConnectionFailure.toLong() -> true
          // Default case
          cause.domain == NSURLErrorDomain && cause.code == NSURLErrorCannotFindHost -> true
          else -> false
        }
    )
  }
}