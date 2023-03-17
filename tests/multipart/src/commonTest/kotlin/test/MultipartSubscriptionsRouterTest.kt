package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import router.DieSubscription
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * A test that runs against https://github.com/apollographql/federated-subscriptions-poc
 */
@Ignore
class MultipartSubscriptionsRouterTest {
  @Test
  fun routerTest() = runTest {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:4040/")
        .subscriptionNetworkTransport(
            HttpNetworkTransport.Builder()
                .addHttpHeader(DefaultHttpRequestComposer.HEADER_ACCEPT_NAME, DefaultHttpRequestComposer.HEADER_ACCEPT_VALUE_MULTIPART)
                .serverUrl("http://localhost:4040/")
                .build()
        )
        .build()

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
      } else {
        it.exception!!.printStackTrace()
      }
    }
  }
}
