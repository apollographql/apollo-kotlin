package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.testing.runTest
import com.benasher44.uuid.uuid4
import defer.WithFragmentSpreadsQuery
import defer.WithInlineFragmentsQuery
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import kotlinx.coroutines.flow.toList
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

  @Test
  fun deferWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"hasNext":true}""",
        """{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1],"hasNext":true}""",
        """{"data":{"isColor":false},"path":["computers",0,"screen"],"hasNext":true,"label":"a"}""",
        """{"data":{"isColor":true},"path":["computers",1,"screen"],"hasNext":false,"label":"a"}""",
    )

    val expectedDataList = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480", null))),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480", null))),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600", null))),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)))),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600", null))),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)))),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)))),
            )
        ),
    )

    mockServer.enqueueMultipart(jsonList)
    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"hasNext":true}""",
        """{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1],"hasNext":true}""",
        """{"data":{"isColor":false},"path":["computers",0,"screen"],"hasNext":true,"label":"b"}""",
        """{"data":{"isColor":true},"path":["computers",1,"screen"],"hasNext":false,"label":"b"}""",
    )

    val expectedDataList = listOf(
        WithInlineFragmentsQuery.Data(
            listOf(
                WithInlineFragmentsQuery.Computer("Computer", "Computer1", null),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithInlineFragmentsQuery.Data(
            listOf(
                WithInlineFragmentsQuery.Computer("Computer", "Computer1", WithInlineFragmentsQuery.OnComputer("386", 1993,
                    WithInlineFragmentsQuery.Screen("Screen", "640x480", null))),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithInlineFragmentsQuery.Data(
            listOf(
                WithInlineFragmentsQuery.Computer("Computer", "Computer1", WithInlineFragmentsQuery.OnComputer("386", 1993,
                    WithInlineFragmentsQuery.Screen("Screen", "640x480", null))),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", WithInlineFragmentsQuery.OnComputer("486", 1996,
                    WithInlineFragmentsQuery.Screen("Screen", "800x600", null))),
            )
        ),
        WithInlineFragmentsQuery.Data(
            listOf(
                WithInlineFragmentsQuery.Computer("Computer", "Computer1", WithInlineFragmentsQuery.OnComputer("386", 1993,
                    WithInlineFragmentsQuery.Screen("Screen", "640x480",
                        WithInlineFragmentsQuery.OnScreen(false)))),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", WithInlineFragmentsQuery.OnComputer("486", 1996,
                    WithInlineFragmentsQuery.Screen("Screen", "800x600", null))),
            )
        ),
        WithInlineFragmentsQuery.Data(
            listOf(
                WithInlineFragmentsQuery.Computer("Computer", "Computer1", WithInlineFragmentsQuery.OnComputer("386", 1993,
                    WithInlineFragmentsQuery.Screen("Screen", "640x480",
                        WithInlineFragmentsQuery.OnScreen(false)))),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", WithInlineFragmentsQuery.OnComputer("486", 1996,
                    WithInlineFragmentsQuery.Screen("Screen", "800x600",
                        WithInlineFragmentsQuery.OnScreen(true)))),
            )
        ),
    )

    mockServer.enqueueMultipart(jsonList)
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithFragmentSpreadsAndError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"hasNext":true}""",
        """{"data":null,"path":["computers",0,"screen"],"label":"b","errors":[{"message":"Cannot resolve isColor","locations":[{"line":1,"column":119}],"path":["computers",0,"screen","isColor"]}],"hasNext":true}""",
        """{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1],"hasNext":true}""",
        """{"data":{"isColor":true},"path":["computers",1,"screen"],"hasNext":false,"label":"a"}""",
    )

    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
                )
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                        ComputerFields.Screen("Screen", "640x480", null))),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
                )
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                        ComputerFields.Screen("Screen", "640x480", null))),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
                )
            )
        )
            .errors(
                listOf(
                    Error(
                        message = "Cannot resolve isColor",
                        locations = listOf(Error.Location(1, 119)),
                        path = listOf("computers", 0, "screen", "isColor"),
                        extensions = null, nonStandardFields = null
                    )
                )
            )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                        ComputerFields.Screen("Screen", "640x480", null))),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                        ComputerFields.Screen("Screen", "800x600", null))),
                )
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                        ComputerFields.Screen("Screen", "640x480", null))),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                        ComputerFields.Screen("Screen", "800x600",
                            ScreenFields(true)))),
                )
            )
        ).build(),
    )

    mockServer.enqueueMultipart(jsonList)
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }
}
