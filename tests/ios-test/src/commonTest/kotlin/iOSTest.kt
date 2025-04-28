
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlin.test.Test

class iOSTest {
  @Test
  fun canRunIOSTest() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()

    // What is passed does not matter
    mockServer.enqueueString("foo")

    apolloClient.close()
    mockServer.close()
  }
}

