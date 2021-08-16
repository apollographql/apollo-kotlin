import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.android.ApolloIdlingResource
import com.apollographql.apollo3.android.withIdlingResource
import idling.resource.IdlingResourceQuery
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.Executors

class IdlingResourceTest {
  @Test
  fun idlingResourceSingleQuery() = runBlocking {
    val mockServer = MockServer()
    mockServer.enqueue(
        MockResponse(
            statusCode = 500,
            delayMs = 500,
        )
    )

    val idlingResource = ApolloIdlingResource("test")
    val apolloClient = ApolloClient(mockServer.url())
        .withIdlingResource(idlingResource)

    launch(start = CoroutineStart.UNDISPATCHED) {
      kotlin.runCatching {
        apolloClient.query(IdlingResourceQuery())
      }
    }
    assert(!idlingResource.isIdleNow)
    delay(300)
    assert(!idlingResource.isIdleNow)
    delay(500)
    assert(idlingResource.isIdleNow)
  }
}