@file:Suppress("DEPRECATION")

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.android.ApolloIdlingResource
import com.apollographql.apollo.android.idlingResource
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
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
    mockServer.enqueue(MockResponse.Builder().statusCode(500).delayMillis(500).build())

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
    delay(5000)
    assert(idlingResource.isIdleNow)
  }
}