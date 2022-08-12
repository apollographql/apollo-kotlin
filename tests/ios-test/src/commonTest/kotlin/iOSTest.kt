
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.GlobalBuilder
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.enqueueData
import com.apollographql.apollo3.testing.internal.runTest
import ios.test.type.buildQuery
import kotlin.test.Test

class iOSTest {
  @Test
  fun canRunIOSTest() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()

    mockServer.enqueueData(GlobalBuilder.buildQuery { random = 42 })

    apolloClient.close()
    mockServer.stop()
  }
}

