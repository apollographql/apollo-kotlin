package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import defer.WithFragmentSpreadsQuery
import defer.WithFragmentSpreadsSubscription
import defer.WithInlineFragmentsQuery
import defer.WithInlineFragmentsSubscription
import defer.fragment.ComputerFields
import defer.fragment.CounterFields
import defer.fragment.ScreenFields
import graphql.GraphQLBoolean
import graphql.GraphQLDeferDirective
import graphql.GraphQLField
import graphql.GraphQLID
import graphql.GraphQLInt
import graphql.GraphQLList
import graphql.GraphQLNonNull
import graphql.GraphQLObjectType
import graphql.GraphQLSchema
import graphql.GraphQLSchemaConfig
import graphql.GraphQLStreamDirective
import graphql.GraphQLString
import helix.HelixServer
import kotlinx.coroutines.flow.toList
import util.dynamicObject
import util.jsAsyncIterator
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Ignore
// TODO These tests are temporarily ignored as the latest version of graphql-js
// uses a package format which is incompatible with Kotlin/JS.
// See https://youtrack.jetbrains.com/issue/KT-12784
class DeferWithHelixTest {
  private lateinit var helixServer: HelixServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    helixServer = HelixServer(schema, port = Random.nextInt(1024, 65535))
    apolloClient = ApolloClient.Builder()
        .serverUrl(helixServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(helixServer.webSocketUrl())
                .build()
        )
        .build()
  }

  private suspend fun tearDown() {
    helixServer.stop()
  }

  private val schema = GraphQLSchema(
      GraphQLSchemaConfig(
          query = GraphQLObjectType(
              name = "Query",
              fields = dynamicObject {
                computers = GraphQLField(
                    type = GraphQLNonNull(GraphQLList(GraphQLNonNull(GraphQLObjectType(
                        name = "Computer",
                        fields = dynamicObject {
                          id = GraphQLField(GraphQLNonNull(GraphQLID))
                          cpu = GraphQLField(GraphQLNonNull(GraphQLString))
                          year = GraphQLField(GraphQLNonNull(GraphQLInt))
                          screen = GraphQLField(GraphQLNonNull(GraphQLObjectType(
                              name = "Screen",
                              fields = dynamicObject {
                                resolution = GraphQLField(GraphQLNonNull(GraphQLString))
                                isColor = GraphQLField(GraphQLNonNull(GraphQLBoolean))
                              }
                          )))
                        }
                    )))),
                    resolve = { _: dynamic, _: dynamic ->
                      JSON.parse<Any>(
                          //language=JSON
                          """
                          [
                            {
                              "id": "Computer1",
                              "cpu": "386",
                              "year": 1993,
                              "screen": {
                                "resolution": "640x480",
                                "isColor": false
                              }
                            },
                            {
                              "id": "Computer2",
                              "cpu": "486",
                              "year": 1996,
                              "screen": {
                                "resolution": "800x600",
                                "isColor": true
                              }
                            }
                          ]
                          """.trimIndent()
                      )
                    }
                )
              }
          ),
          subscription = GraphQLObjectType(
              name = "Subscription",
              fields = dynamicObject {
                count = GraphQLField(
                    type = GraphQLNonNull(GraphQLObjectType(
                        name = "Counter",
                        fields = dynamicObject {
                          value = GraphQLField(GraphQLNonNull(GraphQLInt))
                          valueTimesTwo = GraphQLField(GraphQLNonNull(GraphQLInt))
                        }
                    )),
                    args = dynamicObject {
                      to = dynamicObject {
                        type = GraphQLNonNull(GraphQLInt)
                      }
                    },

                    subscribe = { _: dynamic, args: dynamic ->
                      val to = args.to as Int
                      var countValue = 0

                      jsAsyncIterator(
                          next = {
                            countValue++
                            dynamicObject {
                              count = dynamicObject {
                                this.value = countValue
                                valueTimesTwo = countValue * 2
                              }
                            }
                          },
                          hasNext = { countValue < to }
                      )
                    }
                )
              }
          ),
          directives = arrayOf(GraphQLDeferDirective, GraphQLStreamDirective)
      )
  )

  @Test
  fun queryWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
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

    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun queryWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
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
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun subscriptionWithInlineFragment() = runTest(before = { setUp() }, after = { tearDown() }) {
    val expectedDataList = listOf(
        // Emission 0, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 1, null)),
        // Emission 0, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 1, WithInlineFragmentsSubscription.OnCounter(2))),
        // Emission 1, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 2, null)),
        // Emission 1, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 2, WithInlineFragmentsSubscription.OnCounter(4))),
        // Emission 2, deferred payload 0
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 3, null)),
        // Emission 2, deferred payload 1
        WithInlineFragmentsSubscription.Data(WithInlineFragmentsSubscription.Count("Counter", 3, WithInlineFragmentsSubscription.OnCounter(6))),
    )

    val actualDataList = apolloClient.subscription(WithInlineFragmentsSubscription()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun subscriptionWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
    val expectedDataList = listOf(
        // Emission 0, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 1, null)),
        // Emission 0, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 1, CounterFields(2))),
        // Emission 1, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 2, null)),
        // Emission 1, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 2, CounterFields(4))),
        // Emission 2, deferred payload 0
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 3, null)),
        // Emission 2, deferred payload 1
        WithFragmentSpreadsSubscription.Data(WithFragmentSpreadsSubscription.Count("Counter", 3, CounterFields(6))),
    )

    val actualDataList = apolloClient.subscription(WithFragmentSpreadsSubscription()).toFlow().toList().map { it.dataOrThrow() }
    assertEquals(expectedDataList, actualDataList)
  }
}
