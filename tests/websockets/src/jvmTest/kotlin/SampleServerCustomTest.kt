
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.ackMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SampleServerCustomTest {

}
