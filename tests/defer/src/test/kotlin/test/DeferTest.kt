package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runTest
import defer.Query1Query
import defer.Query2Query
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import defer.test.Query1Query_TestBuilder.Data
import defer.test.Query2Query_TestBuilder.Data
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertContains
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

  @Test
  fun deferDirectivesHaveLabelArgument() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query1 = Query1Query()
    mockServer.enqueue(query1, Query1Query.Data { })
    apolloClient.query(query1).execute()
    var request = mockServer.takeRequest().body.utf8()
    assertContains(request, """...ComputerFields @defer(label: \"query:Query1:0\")""")
    assertContains(request, """...ScreenFields @defer(label: \"fragment:ComputerFields:0\")""")

    val query2 = Query2Query()
    mockServer.enqueue(query2, Query2Query.Data { })
    apolloClient.query(query2).execute()
    request = mockServer.takeRequest().body.utf8()
    assertContains(request, """on Computer @defer(label: \"query:Query2:0\")""")
    assertContains(request, """on Screen @defer(label: \"query:Query2:1\")""")
  }

}
