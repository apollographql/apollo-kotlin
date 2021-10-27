
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test

class iOSTest {
  @Test
  fun canRunIOSTest() = runTest() {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
    val response = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()
    mockServer.enqueue(response)

    apolloClient.dispose()
    mockServer.stop()
  }
}

