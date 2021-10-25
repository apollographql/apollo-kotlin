import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runTest
import fullstack.tutorial.TripsBookedSubscription
import kotlinx.coroutines.flow.collect
import kotlin.test.Ignore
import kotlin.test.Test

// Ignored because it depends on a remote server
// This test requires to book a trip using playground https://apollo-fullstack-tutorial.herokuapp.com/
// See https://github.com/martinbonnin/graphql-ws-server
@Ignore
class FullStackTest {
  @Test
  fun simple() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport(
                serverUrl = "https://apollo-fullstack-tutorial.herokuapp.com/graphql"
            )
        )
        .build()

    apolloClient.subscribe(TripsBookedSubscription())
        .execute()
        .collect {
          println("trips booked: ${it.data?.tripsBooked}")
        }
  }
}