package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.testing.runTest
import defer.Query1Query
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import kotlinx.coroutines.flow.toList
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class DeferTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  /**
   * TODO: This tests the preliminary Multipart support in HttpNetworkTransport which can receive several **full** payloads for now.
   * This can never happen in real life - this test must be updated when partial responses with @defer are supported.
   * Thus, ignoring this test for now.
   */
  @Ignore("Needs to be updated when partial responses with @defer are supported")
  @Test
  fun multipleBodies() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = Query1Query()
    val expectedDataList = listOf(
        Query1Query.Data(
            listOf(
                Query1Query.Computer(
                    __typename = "Computer",
                    id = "computer1",
                    computerFields = ComputerFields(
                        cpu = "386",
                        year = 1993,
                        screen = ComputerFields.Screen(
                            __typename = "Screen",
                            resolution = "640x480",
                            screenFields = ScreenFields(false)
                        )
                    )
                )
            )
        ),
        Query1Query.Data(
            listOf(
                Query1Query.Computer(
                    __typename = "Computer",
                    id = "computer2",
                    computerFields = ComputerFields(
                        cpu = "486",
                        year = 1996,
                        screen = ComputerFields.Screen(
                            __typename = "Screen",
                            resolution = "800x600",
                            screenFields = ScreenFields(true)
                        )
                    )
                )
            )
        )
    )
    val jsonList = expectedDataList.map { data ->
      buildJsonString {
        query.composeJsonResponse(jsonWriter = this, data = data)
      }
    }
    mockServer.enqueueMultipart(jsonList)

    val actualDataList = apolloClient.query(query).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }
}
