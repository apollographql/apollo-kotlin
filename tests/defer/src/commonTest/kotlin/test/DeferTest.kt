package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.autoPersistedQueryInfo
import com.apollographql.apollo.mpp.currentTimeMillis
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueMultipart
import com.apollographql.mockserver.enqueueString
import com.apollographql.mockserver.enqueueStrings
import com.benasher44.uuid.uuid4
import defer.SimpleDeferQuery
import defer.WithFragmentSpreadsQuery
import defer.WithInlineFragmentsQuery
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeferTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()
  }

  private fun tearDown() {
    mockServer.close()
  }

  @Test
  fun deferWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"a"}],"hasNext":false}""",
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

    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"b"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"b"}],"hasNext":false}""",
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

    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithFragmentSpreadsAndError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":null,"path":["computers",0,"screen"],"label":"b","errors":[{"message":"Cannot resolve isColor","locations":[{"line":1,"column":119}],"path":["computers",0,"screen","isColor"]}]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"a"}],"hasNext":false}""",
    )

    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        )).build(),

        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            WithFragmentSpreadsQuery.Data(
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
        )
            .data(
                WithFragmentSpreadsQuery.Data(
                    listOf(
                        WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                            ComputerFields.Screen("Screen", "640x480", null))),
                        WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
                    )
                )
            )
            .errors(
                listOf(
                    Error.Builder(message = "Cannot resolve isColor")
                        .locations(listOf(Error.Location(1, 119)))
                        .path(listOf("computers", 0, "screen", "isColor"))
                        .build()
                )
            )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            WithFragmentSpreadsQuery.Data(
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
        ).data(
            WithFragmentSpreadsQuery.Data(
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

    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun payloadsAreReceivedIncrementally() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    if (com.apollographql.apollo.testing.platform() == com.apollographql.apollo.testing.Platform.Js) {
      // TODO For now chunked is not supported on JS - remove this check when it is
      return@runTest
    }
    val delayMillis = 200L

    val multipartBody = mockServer.enqueueMultipart("application/json")

    val syncChannel = Channel<Unit>()
    val job = launch {
      apolloClient.query(WithFragmentSpreadsQuery()).toFlow().collect {
        syncChannel.send(Unit)
      }
    }

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"a"}],"hasNext":false}""",
    )

    jsonList.withIndex().forEach { (index, value) ->
      multipartBody.enqueueDelay(delayMillis)
      multipartBody.enqueuePart(value.encodeUtf8(), index == jsonList.lastIndex)

      val timeBeforeReceive = currentTimeMillis()
      syncChannel.receive()
      assertTrue(currentTimeMillis() - timeBeforeReceive >= delayMillis)
    }

    job.cancel()
  }

  @Test
  fun emptyPayloadsAreIgnored() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonWithEmptyPayload = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386"},"path":["computers",0]}],"hasNext":true}""",
        """{"hasNext":false}""",
    )
    val jsonWithoutEmptyPayload = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386"},"path":["computers",0]}],"hasNext":false}""",
    )

    val expectedDataList = listOf(
        SimpleDeferQuery.Data(
            listOf(SimpleDeferQuery.Computer("Computer", "computer1", null))
        ),
        SimpleDeferQuery.Data(
            listOf(SimpleDeferQuery.Computer("Computer", "computer1", SimpleDeferQuery.OnComputer("386")))
        ),
    )

    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonWithEmptyPayload)
    var actualDataList = apolloClient.query(SimpleDeferQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)

    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonWithoutEmptyPayload)
    actualDataList = apolloClient.query(SimpleDeferQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithApqFound() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries()
        .build()

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val finalResponse = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().last()
    assertEquals(true, finalResponse.autoPersistedQueryInfo?.hit)
    assertEquals(
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
        finalResponse.dataOrThrow()
    )
  }

  @Test
  fun deferWithApqNotFound() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries()
        .build()

    mockServer.enqueueString("""{"errors":[{"message":"PersistedQueryNotFound"}]}""")
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",1]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",1,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val finalResponse = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().last()
    assertEquals(false, finalResponse.autoPersistedQueryInfo?.hit)
    assertEquals(
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
        finalResponse.dataOrThrow()
    )
  }
}
