import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runTest
import graphql.ws.GreetingsSubscription
import graphql.ws.HelloQuery
import graphql.ws.SetHelloMutation
import kotlinx.coroutines.flow.toList
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

// Ignored because it depends on a local server
// See https://github.com/martinbonnin/graphql-ws-server
@Ignore
class GraphQLWsTest {
  @Test
  fun queryOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport(
                serverUrl = "http://localhost:9090/graphql",
                protocolFactory = GraphQLWsProtocol.Factory()
            )
        )
        .build()

    assertEquals("Hello World!", apolloClient.query(HelloQuery()).data?.hello)
  }

  @Test
  fun mutationOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport(
                serverUrl = "http://localhost:9090/graphql",
                protocolFactory = GraphQLWsProtocol.Factory()
            )
        )
        .build()

    assertEquals("Hello Mutation!", apolloClient.mutate(SetHelloMutation()).data?.hello)
  }


  @Test
  fun subscriptionOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport(
                serverUrl = "http://localhost:9090/graphql",
                protocolFactory = GraphQLWsProtocol.Factory()
            )
        )
        .build()

    val list = apolloClient.subscribe(GreetingsSubscription()).toList()
    assertEquals(listOf("Hi", "Bonjour", "Hola", "Ciao", "Zdravo"), list.map { it.data?.greetings })
  }
}