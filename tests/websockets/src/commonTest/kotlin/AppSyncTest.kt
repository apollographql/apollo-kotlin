import appsync.CommentsSubscription
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.AppSyncWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.runTest
import fullstack.tutorial.TripsBookedSubscription
import kotlinx.coroutines.flow.collect
import kotlin.test.Ignore
import kotlin.test.Test

// Ignored because it depends on a remote server
// This test requires to add a comment using the AWS console at https://eu-west-3.console.aws.amazon.com/appsync/home
// The AWS project requires authentication at the moment. Would be nice to remove this
@Ignore
class AppSyncTest {

  @Test
  fun simple() = runTest {
    val apiKey = "changeMe"
    val host = "6l5lltvi6fgmrpx5abfxrtq6wu.appsync-api.eu-west-3.amazonaws.com"

    val authorization = mapOf(
        "host" to host,
        "x-api-key" to apiKey
    )
    val url = AppSyncWsProtocol.buildUrl(
        baseUrl = "https://6l5lltvi6fgmrpx5abfxrtq6wu.appsync-realtime-api.eu-west-3.amazonaws.com/",
        authorization = authorization
    )
    val apolloClient = ApolloClient(
        networkTransport = WebSocketNetworkTransport(
            serverUrl = url,
            protocolFactory = AppSyncWsProtocol.Factory(
                authorization = authorization
            )
        )
    )

    apolloClient.subscribe(CommentsSubscription())
        .collect {
          println("comment: ${it.data?.subscribeToEventComments?.content}")
        }
  }
}