
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
// These tests take a long time to execute, this is expected
@Ignore
class GraphQLWsTest {
  @Test
  fun queryOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport.Builder().serverUrl(
                serverUrl = "http://localhost:9090/graphql",
            ).protocol(
                protocolFactory = GraphQLWsProtocol.Factory()
            ).build()
        )
        .build()

    assertEquals("Hello World!", apolloClient.query(HelloQuery()).execute().data?.hello)
  }

  @Test
  fun mutationOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport.Builder().serverUrl(
                serverUrl = "http://localhost:9090/graphql",
            ).protocol(
                protocolFactory = GraphQLWsProtocol.Factory()
            ).build()
        )
        .build()

    assertEquals("Hello Mutation!", apolloClient.mutation(SetHelloMutation()).execute().data?.hello)
  }


  @Test
  fun subscriptionOverWebSocket() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport.Builder().serverUrl(
                serverUrl = "http://localhost:9090/graphql",
            ).protocol(
                protocolFactory = GraphQLWsProtocol.Factory()
            ).build()
        )
        .build()

    val list = apolloClient.subscription(GreetingsSubscription())
        .toFlow()
        .toList()
    assertEquals(listOf("Hi", "Bonjour", "Hola", "Ciao", "Zdravo"), list.map { it.data?.greetings })

    apolloClient.dispose()
  }
}
