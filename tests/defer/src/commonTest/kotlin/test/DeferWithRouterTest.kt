package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.testing.internal.runTest
import defer.CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery
import defer.CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery
import defer.CanDeferFragmentsOnTheTopLevelQueryFieldQuery
import defer.CanDisableDeferUsingIfArgumentQuery
import defer.DoesNotDisableDeferWithNullIfArgumentQuery
import defer.WithFragmentSpreadsQuery
import defer.WithInlineFragmentsQuery
import defer.fragment.ComputerFields
import defer.fragment.FragmentOnQuery
import defer.fragment.ScreenFields
import kotlinx.coroutines.flow.toList
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * End-to-end tests for `@defer`.
 *
 * These tests are not run by default (they are excluded in the gradle conf) because they expect an instance of
 * [Apollo Router](https://www.apollographql.com/docs/router/) running locally.
 *
 * They are enabled only when running from the specific `defer-with-router-tests` CI workflow.
 */
class DeferWithRouterTest {
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    apolloClient = ApolloClient.Builder()
        .httpEngine(getStreamingHttpEngine())
        .serverUrl("http://localhost:4000/")
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
  }

  @Test
  fun deferWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"id":"Computer1"},{"id":"Computer2"}]},"hasNext":true}
    // {"hasNext":true,"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"resolution":"640x480"}},"path":["computers",0]},{"data":{"cpu":"486","year":1996,"screen":{"resolution":"800x600"}},"path":["computers",1]}]}
    // {"hasNext":false,"incremental":[{"label":"a","data":{"isColor":false},"path":["computers",0,"screen"]},{"label":"a","data":{"isColor":true},"path":["computers",1,"screen"]}]}
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

    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"id":"Computer1"},{"id":"Computer2"}]},"hasNext":true}
    // {"hasNext":true,"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"resolution":"640x480"}},"path":["computers",0]},{"data":{"cpu":"486","year":1996,"screen":{"resolution":"800x600"}},"path":["computers",1]}]}
    // {"hasNext":false,"incremental":[{"label":"b","data":{"isColor":false},"path":["computers",0,"screen"]},{"label":"b","data":{"isColor":true},"path":["computers",1,"screen"]}]}
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
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun canDisableDeferUsingIfArgument() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"id":"Computer1","cpu":"386"},{"id":"Computer2","cpu":"486"}]}}
    val expectedDataList = listOf(
        CanDisableDeferUsingIfArgumentQuery.Data(
            listOf(
                CanDisableDeferUsingIfArgumentQuery.Computer("Computer", "Computer1", CanDisableDeferUsingIfArgumentQuery.OnComputer("386")),
                CanDisableDeferUsingIfArgumentQuery.Computer("Computer", "Computer2", CanDisableDeferUsingIfArgumentQuery.OnComputer("486")),
            )
        ),
    )
    val actualDataList = apolloClient.query(CanDisableDeferUsingIfArgumentQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun doesNotDisableDeferWithNullIfArgument() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"id":"Computer1","cpu":"386"},{"id":"Computer2","cpu":"486"}]},"hasNext":false}
    val expectedDataList = listOf(
        DoesNotDisableDeferWithNullIfArgumentQuery.Data(
            listOf(
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer1", DoesNotDisableDeferWithNullIfArgumentQuery.OnComputer("386")),
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer2", DoesNotDisableDeferWithNullIfArgumentQuery.OnComputer("486")),
            )
        ),
    )
    val actualDataList = apolloClient.query(DoesNotDisableDeferWithNullIfArgumentQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  @Ignore
  // TODO Ignored for now, currently not supported by Router - see https://github.com/apollographql/router/issues/1800
  fun canDeferFragmentsOnTheTopLevelQueryField() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{},"hasNext":true}
    // {"incremental":[{"data":{"computers":[{"id":"Computer1"},{"id":"Computer2"}]},"path":[]}],"hasNext":false}
    val expectedDataList = listOf(
        CanDeferFragmentsOnTheTopLevelQueryFieldQuery.Data(
            "Query",
            null
        ),
        CanDeferFragmentsOnTheTopLevelQueryFieldQuery.Data(
            "Query",
            FragmentOnQuery(
                listOf(
                    FragmentOnQuery.Computer("Computer1"),
                    FragmentOnQuery.Computer("Computer2"),
                )
            )
        ),
    )
    val actualDataList = apolloClient.query(CanDeferFragmentsOnTheTopLevelQueryFieldQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun canDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"screen":{"isColor":false}}},"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"isColor":false},"path":["computer","screen"]}]}
    val expectedDataList = listOf(
        CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Data(
            CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Computer(
                CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Screen("Screen", ScreenFields(false))
            )
        ),
        CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Data(
            CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Computer(
                CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery.Screen("Screen", ScreenFields(false))
            )
        ),
    )
    val actualDataList = apolloClient.query(CanDeferAFragmentThatIsAlsoNotDeferredDeferredFragmentIsFirstQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun canDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"screen":{"isColor":false}}},"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"isColor":false},"path":["computer","screen"]}]}
    val expectedDataList = listOf(
        CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Data(
            CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Computer(
                CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Screen("Screen", ScreenFields(false))
            )
        ),
        CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Data(
            CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Computer(
                CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery.Screen("Screen", ScreenFields(false))
            )
        ),
    )
    val actualDataList = apolloClient.query(CanDeferAFragmentThatIsAlsoNotDeferredNotDeferredFragmentIsFirstQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

}
