
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.ws.GraphQLWsProtocol
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import graphql.ws.GreetingsSubscription
import graphql.ws.HelloQuery
import graphql.ws.Hello2Query
import graphql.ws.SetHelloMutation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import okio.use
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Ignored because it depends on a local server
// See https://github.com/martinbonnin/graphql-ws-server
// These tests take a long time to execute, this is expected
@Ignore
class GraphQLWsTest {
  private fun withApolloClient(block: suspend CoroutineScope.(ApolloClient) -> Unit) = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(
            WebSocketNetworkTransport.Builder().serverUrl(
                serverUrl = "http://localhost:9090/graphql",
            ).protocol(
                protocolFactory = GraphQLWsProtocol.Factory()
            ).build()
        )
        .build()
    apolloClient.use {
      block(it)
    }
  }
  @Test
  fun queryOverWebSocket() = withApolloClient { apolloClient ->
    assertEquals("Hello World!", apolloClient.query(HelloQuery()).execute().data?.hello)
  }

  @Test
  fun errorOverWebSocket()  = withApolloClient { apolloClient ->
    val response = apolloClient.query(Hello2Query()).execute()
    assertNull(response.data)
    assertEquals("Cannot query field \"hello2\" on type \"Query\". Did you mean \"hello\"?", response.errors?.get(0)?.message)
  }

  @Test
  fun mutationOverWebSocket() =  withApolloClient { apolloClient ->
    assertEquals("Hello Mutation!", apolloClient.mutation(SetHelloMutation()).execute().data?.hello)
  }


  @Test
  fun subscriptionOverWebSocket() = withApolloClient { apolloClient ->
    val list = apolloClient.subscription(GreetingsSubscription())
        .toFlow()
        .toList()
    assertEquals(listOf("Hi", "Bonjour", "Hola", "Ciao", "Zdravo"), list.map { it.data?.greetings })
  }
}
