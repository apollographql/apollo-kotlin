import appsync.CommentsSubscription
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.ws.AppSyncWsProtocol
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Ignore
import kotlin.test.Test

/**
 *
 * This test is Ignored because it depends on a remote server
 * This test requires to add a comment using the AWS console at https://eu-west-3.console.aws.amazon.com/appsync/home
 * It's important that the mutation returns all fields queried by the subscription:
 *
 * mutation {
 *   commentOnEvent(content: "hello", createdAt: "2021-10-11T08:35:23+0000", eventId: "eeafafae-91dc-4951-9fdb-d2df84f3abf3") {
 *     content
 *     eventId
 *   }
 * }
 *
 * To list the comments:
 *
 * query {
 *   getEvent(id: "eeafafae-91dc-4951-9fdb-d2df84f3abf3") {
 *     id
 *     name
 *     comments {
 *       items {
 *         commentId
 *         content
 *         createdAt
 *       }
 *     }
 *   }
 *
 * The AWS project requires authentication at the moment. You can get an apiKey from the console. It would be nice to change this
 */
@Ignore
class AppSyncTest {

  @Suppress("UNREACHABLE_CODE")
  @Test
  fun simple() = runTest {
    @Suppress("UNUSED_VARIABLE")
    val apiKey = TODO("changeMe")
    val host = "6l5lltvi6fgmrpx5abfxrtq6wu.appsync-api.eu-west-3.amazonaws.com"

    fun generateAuth(): Map<String, String?> {
        val authorization = mapOf(
            "host" to host,
            "x-api-key" to apiKey
        )
        return authorization
    }

    val url = AppSyncWsProtocol.buildUrl(
        baseUrl = "https://6l5lltvi6fgmrpx5abfxrtq6wu.appsync-realtime-api.eu-west-3.amazonaws.com/",
        authorization = generateAuth()
    )
    val apolloClient = ApolloClient.Builder().networkTransport(
        networkTransport = WebSocketNetworkTransport.Builder().serverUrl(
            serverUrl = url,
        ).protocol(
            protocolFactory = AppSyncWsProtocol.Factory(
                connectionPayload = { generateAuth() }
            )
        ).build()
    ).build()

    apolloClient.subscription(CommentsSubscription()).toFlow()
        .collect {
          println(it.data)
        }
  }
}
