package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.catch
import okio.use
import router.DieSubscription
import router.ReviewSubscription
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * A test that runs against https://github.com/apollographql/federated-subscriptions-poc
 */
@Ignore
class MultipartSubscriptionsRouterTest {
  private fun client() = ApolloClient.Builder()
      .serverUrl("http://localhost:4040/")
      .subscriptionNetworkTransport(
          HttpNetworkTransport.Builder()
              .serverUrl("http://localhost:4040/")
              .build()
      )
      .build()

  @Test
  fun dieTest() = runTest {
    client().use { apolloClient ->
      /**
       * Should display something like this
       *
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=3, sides=30, color=red)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=5, sides=31, color=blue)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=11, sides=32, color=red)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=12, sides=33, color=blue)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=21, sides=34, color=red)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=16, sides=35, color=blue)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=22, sides=36, color=red)))
       * Got Data(aNewDieWasCreated=ANewDieWasCreated(die=Die(roll=11, sides=37, color=blue)))
       */
      apolloClient.subscription(DieSubscription()).toFlow().collect {
        if (it.data != null) {
          println("${it.data}")
        }
      }
    }
  }

  /**
   * Run 'docker compose kill reviews' to trigger an exception
   */
  @Test
  fun reviewTest() = runTest {
    var exception: Throwable? = null
    client().use { apolloClient ->
      apolloClient.subscription(ReviewSubscription()).toFlow().catch {
        exception = it
      }.collect {
        if (it.data != null) {
          println("${it.data} isLast=${it.isLast}")
        }
      }
    }
    assertIs<SubscriptionOperationException>(exception)
  }
}
