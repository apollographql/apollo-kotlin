
import cloud.router.GetReviewSubscription
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.LoggingInterceptor
import com.apollographql.apollo3.testing.internal.runTest
import fullstack.tutorial.TripsBookedSubscription
import kotlin.test.Ignore
import kotlin.test.Test

// Ignored because it depends on a remote server
@Ignore
class CloudRouterTest {
  @Test
  fun simple() = runTest {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("")
        .subscriptionNetworkTransport(
            HttpNetworkTransport.Builder()
                .serverUrl("https://main--subs-testing.staging.cloud.starstuff.dev/graphql")
                .build()
        )
        .build()

    apolloClient.subscription(GetReviewSubscription())
        .toFlow()
        .collect {
          println("trips booked: ${it.data}")
        }

    apolloClient.close()
  }
}
