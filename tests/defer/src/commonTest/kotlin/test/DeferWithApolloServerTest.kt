package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.testing.internal.runTest
import com.benasher44.uuid.uuid4
import defer.CanDeferFragmentsOnTheTopLevelQueryFieldQuery
import defer.CanDisableDeferUsingIfArgumentQuery
import defer.DeferFragmentThatIsAlsoNotDeferredIsSkipped1Query
import defer.DeferFragmentThatIsAlsoNotDeferredIsSkipped2Query
import defer.DoesNotDisableDeferWithNullIfArgumentQuery
import defer.HandlesErrorsThrownInDeferredFragmentsQuery
import defer.HandlesNonNullableErrorsThrownInDeferredFragmentsQuery
import defer.HandlesNonNullableErrorsThrownOutsideDeferredFragmentsQuery
import defer.Overlapping2Query
import defer.OverlappingQuery
import defer.SubPathQuery
import defer.WithFragmentSpreadsMutation
import defer.WithFragmentSpreadsQuery
import defer.WithInlineFragmentsQuery
import defer.fragment.ComputerErrorField
import defer.fragment.ComputerFields
import defer.fragment.FragmentOnQuery
import defer.fragment.ScreenFields
import defer.notypename.SkippingEmptyFragmentQuery
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * End-to-end tests for `@defer`.
 *
 * These tests are not run by default (they are excluded in the gradle conf) because they expect an instance of
 * [Apollo Server](https://www.apollographql.com/docs/apollo-server) running locally.
 *
 * They are enabled only when running from the specific `defer-with-apollo-server-tests` CI workflow.
 */
class DeferWithApolloServerTest {
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    apolloClient = ApolloClient.Builder()
        .serverUrl("http://127.0.0.1:4000/")
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
  }

  @Test
  fun deferWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}
    // {"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}
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
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)
                    )
                )
                ),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                ),
            )
        ),
    )

    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}
    // {"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"b"},{"id":"3","path":["computers",1,"screen"],"label":"b"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}
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
                    WithInlineFragmentsQuery.Screen("Screen", "640x480",
                        WithInlineFragmentsQuery.OnScreen(false)
                    )
                )
                ),
                WithInlineFragmentsQuery.Computer("Computer", "Computer2", WithInlineFragmentsQuery.OnComputer("486", 1996,
                    WithInlineFragmentsQuery.Screen("Screen", "800x600",
                        WithInlineFragmentsQuery.OnScreen(true)
                    )
                )
                ),
            )
        ),
    )
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithFragmentSpreadsMutation() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0],"label":"c"},{"id":"1","path":["computers",1],"label":"c"}],"hasNext":true}
    // {"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}
    val expectedDataList = listOf(
        WithFragmentSpreadsMutation.Data(
            listOf(
                WithFragmentSpreadsMutation.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsMutation.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsMutation.Data(
            listOf(
                WithFragmentSpreadsMutation.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)
                    )
                )
                ),
                WithFragmentSpreadsMutation.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                ),
            )
        ),
    )

    val actualDataList = apolloClient.mutation(WithFragmentSpreadsMutation()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun canDisableDeferUsingIfArgument() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"__typename":"Computer","id":"Computer1","cpu":"386"},{"__typename":"Computer","id":"Computer2","cpu":"486"}]}
    val expectedDataList = listOf(
        CanDisableDeferUsingIfArgumentQuery.Data(
            listOf(
                CanDisableDeferUsingIfArgumentQuery.Computer("Computer", "Computer1", CanDisableDeferUsingIfArgumentQuery.OnComputer("386")),
                CanDisableDeferUsingIfArgumentQuery.Computer("Computer", "Computer2", CanDisableDeferUsingIfArgumentQuery.OnComputer("486")),
            )
        ),
    )
    val actualDataList = apolloClient.query(CanDisableDeferUsingIfArgumentQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun doesNotDisableDeferWithNullIfArgument() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"cpu":"386"},"id":"0"},{"data":{"cpu":"486"},"id":"1"}],"completed":[{"id":"0"},{"id":"1"}]}
    val expectedDataList = listOf(
        DoesNotDisableDeferWithNullIfArgumentQuery.Data(
            listOf(
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer1", null),
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer2", null),
            )
        ),
        DoesNotDisableDeferWithNullIfArgumentQuery.Data(
            listOf(
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer1", DoesNotDisableDeferWithNullIfArgumentQuery.OnComputer("386")),
                DoesNotDisableDeferWithNullIfArgumentQuery.Computer("Computer", "Computer2", DoesNotDisableDeferWithNullIfArgumentQuery.OnComputer("486")),
            )
        )
    )
    val actualDataList =
      apolloClient.query(DoesNotDisableDeferWithNullIfArgumentQuery(Optional.Absent)).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun canDeferFragmentsOnTheTopLevelQueryField() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"__typename":"Query"},"pending":[{"id":"0","path":[]}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"computers":[{"id":"Computer1"},{"id":"Computer2"}]},"id":"0"}],"completed":[{"id":"0"}]}
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
    val actualDataList = apolloClient.query(CanDeferFragmentsOnTheTopLevelQueryFieldQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferFragmentThatIsAlsoNotDeferredIsSkipped1() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"screen":{"__typename":"Screen","isColor":false}}}}
    val expectedDataList = listOf(
        DeferFragmentThatIsAlsoNotDeferredIsSkipped1Query.Data(
            DeferFragmentThatIsAlsoNotDeferredIsSkipped1Query.Computer(
                DeferFragmentThatIsAlsoNotDeferredIsSkipped1Query.Screen("Screen", ScreenFields(false))
            )
        ),
    )
    val actualDataList = apolloClient.query(DeferFragmentThatIsAlsoNotDeferredIsSkipped1Query()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferFragmentThatIsAlsoNotDeferredIsSkipped2() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"screen":{"__typename":"Screen","isColor":false}}}}
    val expectedDataList = listOf(
        DeferFragmentThatIsAlsoNotDeferredIsSkipped2Query.Data(
            DeferFragmentThatIsAlsoNotDeferredIsSkipped2Query.Computer(
                DeferFragmentThatIsAlsoNotDeferredIsSkipped2Query.Screen("Screen", ScreenFields(false))
            )
        ),
    )
    val actualDataList = apolloClient.query(DeferFragmentThatIsAlsoNotDeferredIsSkipped2Query()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun handlesErrorsThrownInDeferredFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"__typename":"Computer","id":"Computer1"}},"pending":[{"id":"0","path":["computer"]}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"errorField":null},"errors":[{"message":"Error field","locations":[{"line":3,"column":43}],"path":["computer","errorField"],"extensions":{"code":"INTERNAL_SERVER_ERROR","stacktrace":["Error: Error field","    at Object.errorField (file:///Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/computers.js:29:19)","    at field.resolve (file:///Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/@apollo/server/dist/esm/utils/schemaInstrumentation.js:36:28)","    at executeField (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:567:20)","    at executeFields (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:476:22)","    at executeExecutionGroup (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:1855:14)","    at executor (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:1803:7)","    at pendingExecutionGroup.result (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:1825:58)","    at IncrementalGraph._onExecutionGroup (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/IncrementalGraph.js:192:33)","    at IncrementalGraph._promoteNonEmptyToRoot (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/IncrementalGraph.js:146:20)","    at IncrementalGraph.getNewRootNodes (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/IncrementalGraph.js:25:17)"]}}],"id":"0"}],"completed":[{"id":"0"}]}
    val query = HandlesErrorsThrownInDeferredFragmentsQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        )
            .data(
                HandlesErrorsThrownInDeferredFragmentsQuery.Data(
                    HandlesErrorsThrownInDeferredFragmentsQuery.Computer(
                        "Computer", "Computer1", null
                    )
                )
            )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
        )
            .data(
                HandlesErrorsThrownInDeferredFragmentsQuery.Data(
                    HandlesErrorsThrownInDeferredFragmentsQuery.Computer(
                        "Computer", "Computer1", ComputerErrorField(null)
                    )
                )
            )
            .errors(
                listOf(
                    Error.Builder(message = "Error field")
                        .path(listOf("computer", "errorField"))
                        .build()
                )
            )
            .build(),
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun handlesNonNullableErrorsThrownInDeferredFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"__typename":"Computer","id":"Computer1"}},"pending":[{"id":"0","path":["computer"]}],"hasNext":true}
    // {"hasNext":false,"completed":[{"id":"0","errors":[{"message":"Cannot return null for non-nullable field Computer.nonNullErrorField.","locations":[{"line":3,"column":54}],"path":["computer","nonNullErrorField"]}]}]}
    val query = HandlesNonNullableErrorsThrownInDeferredFragmentsQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            HandlesNonNullableErrorsThrownInDeferredFragmentsQuery.Data(
                HandlesNonNullableErrorsThrownInDeferredFragmentsQuery.Computer(
                    "Computer", "Computer1", null
                )
            )
        )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
        )
            .data(
                HandlesNonNullableErrorsThrownInDeferredFragmentsQuery.Data(
                    HandlesNonNullableErrorsThrownInDeferredFragmentsQuery.Computer(
                        "Computer", "Computer1", null
                    )
                )
            )
            .errors(listOf(Error.Builder(message = "Cannot return null for non-nullable field Computer.nonNullErrorField.")
                .path(listOf("computer", "nonNullErrorField")).build()
            )
            )
            .build(),
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun handlesNonNullableErrorsThrownOutsideDeferredFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"errors":[{"message":"Cannot return null for non-nullable field Computer.nonNullErrorField.","locations":[{"line":1,"column":108}],"path":["computer","nonNullErrorField"],"extensions":{"code":"INTERNAL_SERVER_ERROR","stacktrace":["Error: Cannot return null for non-nullable field Computer.nonNullErrorField.","    at completeValue (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:716:13)","    at executeField (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:580:23)","    at executeFields (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:476:22)","    at collectAndExecuteSubfields (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:1491:21)","    at completeObjectValue (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:1395:10)","    at completeValue (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:760:12)","    at executeField (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:580:23)","    at executeFields (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:476:22)","    at executeRootGroupedFieldSet (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:373:14)","    at executeOperation (/Users/bod/gitrepo/apollo-kotlin-0/tests/defer/apollo-server/node_modules/graphql/execution/execute.js:159:30)"]}}],"data":{"computer":null}}
    val query = HandlesNonNullableErrorsThrownOutsideDeferredFragmentsQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            HandlesNonNullableErrorsThrownOutsideDeferredFragmentsQuery.Data(
                null
            )
        )
            .errors(
                listOf(
                    Error.Builder(message = "Cannot return null for non-nullable field Computer.nonNullErrorField.")
                        .path(listOf("computer", "nonNullErrorField"))
                        .build()
                )
            )
            .build()
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun overlapping() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"__typename":"Computer","id":"Computer1"}},"pending":[{"id":"0","path":["computer"],"label":"b"}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"cpu":"386","year":1993},"id":"0"}],"completed":[{"id":"0"}]}
    val query = OverlappingQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            OverlappingQuery.Data(
                OverlappingQuery.Computer(
                    "Computer", "Computer1", OverlappingQuery.OnComputer(
                    "Computer", "Computer1", null,
                )
                )
            )
        )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            OverlappingQuery.Data(
                OverlappingQuery.Computer(
                    "Computer", "Computer1", OverlappingQuery.OnComputer(
                    "Computer", "Computer1", OverlappingQuery.OnComputer1("Computer1", "386", 1993)
                )
                )
            )
        )
            .build()
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun overlapping2() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{"__typename":"Computer","id":"Computer1"}},"pending":[{"id":"0","path":["computer"],"label":"b"}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"cpu":"386","year":1993},"id":"0"}],"completed":[{"id":"0"}]}
    val query = Overlapping2Query()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            Overlapping2Query.Data(
                Overlapping2Query.Computer(
                    "Computer", "Computer1", Overlapping2Query.OnComputerDeferA("Computer1"
                ), null
                )
            )
        )
            .build(),
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            Overlapping2Query.Data(
                Overlapping2Query.Computer(
                    "Computer", "Computer1", Overlapping2Query.OnComputerDeferA("Computer1"
                ), Overlapping2Query.OnComputerDeferB(
                    "Computer1", "386", 1993
                )
                )
            )
        )
            .build()
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun subPath() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"__typename":"Query","computer":{"id":"Computer1"}},"pending":[{"id":"0","path":[],"label":"a"}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"screen":{"isColor":false}},"id":"0","subPath":["computer"]},{"data":{"MyFragment":"Query"},"id":"0"}],"completed":[{"id":"0"}]}
    val query = SubPathQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            SubPathQuery.Data(
                "Query", SubPathQuery.Computer(
                "Computer1"
            ), null
            )
        )
            .build(),
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            SubPathQuery.Data(
                "Query", SubPathQuery.Computer(
                "Computer1"
            ), SubPathQuery.OnQuery(
                "Query", SubPathQuery.Computer1(
                "Computer1",
                SubPathQuery.Screen(false
                )
            )
            )
            )
        )
            .build()
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }

  @Test
  fun skippingEmptyFragment() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Expected payloads:
    // {"data":{"computer":{}},"pending":[{"id":"0","path":["computer"],"label":"c"}],"hasNext":true}
    // {"hasNext":false,"incremental":[{"data":{"id":"Computer1"},"id":"0"}],"completed":[{"id":"0"}]}
    val query = SkippingEmptyFragmentQuery()
    val uuid = uuid4()

    val expectedDataList = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            SkippingEmptyFragmentQuery.Data(
                SkippingEmptyFragmentQuery.Computer(
                    SkippingEmptyFragmentQuery.OnComputer(
                        SkippingEmptyFragmentQuery.OnComputer1(
                            null
                        )
                    )
                )
            )
        )
            .build(),

        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            SkippingEmptyFragmentQuery.Data(
                SkippingEmptyFragmentQuery.Computer(
                    SkippingEmptyFragmentQuery.OnComputer(
                        SkippingEmptyFragmentQuery.OnComputer1(
                            SkippingEmptyFragmentQuery.OnComputer2(
                                "Computer1"
                            )
                        )
                    )
                )
            )
        )
            .build()
    )
    val actualResponseList = apolloClient.query(query).toFlow().toList()
    assertResponseListEquals(expectedDataList, actualResponseList)
  }


}
