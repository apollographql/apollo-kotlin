import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.ApolloWebSocketNetworkTransport
import com.apollographql.apollo3.testing.MainLoopDispatcher
import com.apollographql.apollo3.testing.runWithMainLoop
import fullstack.tutorial.TripsBookedSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlin.test.Ignore
import kotlin.test.Test

class FullStackTest {
  @Test
  @Ignore
  fun simple() {
    val apolloClient = ApolloClient(
        networkTransport = ApolloWebSocketNetworkTransport(
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