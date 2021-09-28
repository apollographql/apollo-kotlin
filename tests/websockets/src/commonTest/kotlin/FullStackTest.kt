import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runWithMainLoop
import fullstack.tutorial.TripsBookedSubscription
import kotlinx.coroutines.flow.collect
import kotlin.test.Ignore
import kotlin.test.Test

class FullStackTest {
  @Test
  @Ignore
  fun simple() {
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = "https://apollo-fullstack-tutorial.herokuapp.com/graphql"
        )
    )

    runWithMainLoop {
      apolloClient.subscribe(TripsBookedSubscription())
          .collect {
            println("trips booked: ${it.data?.tripsBooked}")
          }
    }
  }
}