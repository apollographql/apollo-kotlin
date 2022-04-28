package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.testing.runTest
import defer.WithFragmentSpreadsQuery
import kotlinx.coroutines.flow.collect
import kotlin.test.Test
import kotlin.test.assertTrue

// TODO Move this test to the common test suite when chunked support works on JS
@OptIn(ApolloExperimental::class)
class DeferAppleStreamingTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .httpEngine(getStreamingHttpEngine())
        .serverUrl(mockServer.url())
        .build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun payloadsAreReceivedIncrementally() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"hasNext":true}""",
        """{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1],"hasNext":true}""",
        """{"data":{"isColor":false},"path":["computers",0,"screen"],"hasNext":true,"label":"a"}""",
        """{"data":{"isColor":true},"path":["computers",1,"screen"],"hasNext":false,"label":"a"}""",
    )

    val delay = 200L
    mockServer.enqueueMultipart(jsonList, chunksDelayMillis = delay)

    val actualDelays = mutableListOf<Long>()
    var lastEmitTime = currentTimeMillis()
    apolloClient.query(WithFragmentSpreadsQuery()).toFlow().collect {
      actualDelays += currentTimeMillis() - lastEmitTime
      lastEmitTime = currentTimeMillis()
    }
    // Last 2 emissions can arrive together, so ignore last element
    for (d in actualDelays.dropLast(1)) {
      // Allow a 10% margin for inaccuracies
      assertTrue(d >= delay / 1.1)
    }
  }

}
