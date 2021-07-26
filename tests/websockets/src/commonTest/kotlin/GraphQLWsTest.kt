import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runWithMainLoop
import graphql.ws.GreetingsSubscription
import graphql.ws.HelloQuery
import kotlinx.coroutines.flow.toList
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

// Ignored because it depends on a local server
// See https://github.com/martinbonnin/graphql-ws-server
@Ignore
class GraphQLWsTest {
  @Test
  fun queryOverWebSocket() {
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = "http://localhost:9090/graphql",
            protocol = GraphQLWsProtocol()
        )
    )

    runWithMainLoop {
      assertEquals("Hello World!", apolloClient.query(HelloQuery()).data?.hello)
    }
  }

  @Test
  fun subscriptionOverWebSocket() {
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = "http://localhost:9090/graphql",
            protocol = GraphQLWsProtocol()
        )
    )

    runWithMainLoop {
      val list = apolloClient.subscribe(GreetingsSubscription()).toList()
      assertEquals(listOf("Hi", "Bonjour", "Hola", "Ciao", "Zdravo"), list.map { it.data?.greetings })
    }
  }
}