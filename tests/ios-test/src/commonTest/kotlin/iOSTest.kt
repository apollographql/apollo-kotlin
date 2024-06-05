
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.GlobalBuilder
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import ios.test.type.buildQuery
import kotlin.test.Test

class iOSTest {
  @Test
  fun canRunIOSTest() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()

    // What is passed does not matter
    mockServer.enqueueString(GlobalBuilder.buildQuery { random = 42 }.toString())

    apolloClient.close()
    mockServer.close()
  }
}

