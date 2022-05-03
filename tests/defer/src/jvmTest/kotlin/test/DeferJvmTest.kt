package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.http.httpCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.testing.runTest
import defer.WithFragmentSpreadsQuery
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class DeferJvmTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpCache(dir, 4_000)
        .build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun payloadsAreReceivedIncrementallyWithHttpCache() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"hasNext":true}""",
        """{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1],"hasNext":true}""",
        """{"data":{"isColor":false},"path":["computers",0,"screen"],"hasNext":true,"label":"a"}""",
        """{"data":{"isColor":true},"path":["computers",1,"screen"],"hasNext":false,"label":"a"}""",
    )

    val delay = 200L
    mockServer.enqueueMultipart(jsonList, chunksDelayMillis = delay)

    var lastEmitTime = currentTimeMillis()
    apolloClient.query(WithFragmentSpreadsQuery()).toFlow().collect {
      // Allow a 10% margin for inaccuracies
      assertTrue(currentTimeMillis() - lastEmitTime >= delay / 1.1)
      lastEmitTime = currentTimeMillis()
    }
  }
}
