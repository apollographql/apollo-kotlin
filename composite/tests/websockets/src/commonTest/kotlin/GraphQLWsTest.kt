import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.GraphQLWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runWithMainLoop
import graphql.ws.HelloQuery
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphQLWsTest {
  @Test
  @Ignore
  // Ignored because it depends on a local server
  // See https://github.com/martinbonnin/graphql-ws-server
  fun test() {
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
}