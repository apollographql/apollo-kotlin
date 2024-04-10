
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import sample.server.CountSubscription

class SampleServerCustomTest {
  @Test
  fun websocketReopenWhenDoesNotPile() {

    val port = 56678
    val url = "http://localhost:$port/subscriptions"
    var sampleServer: SampleServer? = SampleServer(port)

    var reopenCount = 0

    val apolloClient = ApolloClient.Builder()
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(url)
                .reopenWhen { _, _ ->
                  delay(1000)
                  reopenCount++
                  true
                }
                .build()
        )
        .serverUrl("https://unused.com/")
        .build()

    runBlocking {
      repeat(50) { _ ->
        launch {
          try {
            withTimeout(30_000) {
              apolloClient.subscription(CountSubscription(Int.MAX_VALUE, 3600 * 1000))
                  .toFlow()
                  .take(2)
                  .collect()
            }
          } catch (e: TimeoutCancellationException) {
            error("timeout")
          }
        }
      }
      launch {
        delay(1000)
        sampleServer!!.close()
        delay(10_000)
        sampleServer = SampleServer(port)
      }
    }
    sampleServer?.close()
  }
}
