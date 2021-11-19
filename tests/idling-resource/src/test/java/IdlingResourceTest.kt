import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.android.ApolloIdlingResource
import com.apollographql.apollo3.android.idlingResource
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import idling.resource.IdlingResourceQuery
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class IdlingResourceTest {
  @Test
  fun idlingResourceSingleQuery() = runBlocking {
    val mockServer = MockServer()
    mockServer.enqueue(
        MockResponse(
            statusCode = 500,
            delayMillis = 500,
        )
    )

    val idlingResource = ApolloIdlingResource("test")
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .idlingResource(idlingResource)
        .build()

    launch(start = CoroutineStart.UNDISPATCHED) {
      kotlin.runCatching {
        apolloClient.query(IdlingResourceQuery()).execute()
      }
    }
    assert(!idlingResource.isIdleNow)
    delay(300)
    assert(!idlingResource.isIdleNow)
    delay(500)
    assert(idlingResource.isIdleNow)
  }
}