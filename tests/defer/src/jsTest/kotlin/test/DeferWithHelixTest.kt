package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.testing.runTest
import defer.WithFragmentSpreadsQuery
import defer.WithInlineFragmentsQuery
import defer.fragment.ComputerFields
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
import kotlin.test.Test
import kotlin.test.assertEquals

class DeferWithHelixTest {
  private lateinit var helixServer: HelixServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    helixServer = HelixServer(schema)
    apolloClient = ApolloClient.Builder().serverUrl(helixServer.url()).build()
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
                          """[
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
                                ]""".trimIndent()
                      )
                    }
                )
              }

          ),
          directives = arrayOf(GraphQLDeferDirective, GraphQLStreamDirective)
      )
  )

  @Test
  fun deferWithFragmentSpreads() = runTest(before = { setUp() }, after = { tearDown() }) {
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

    val actualDataList = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }

  @Test
  fun deferWithInlineFragments() = runTest(before = { setUp() }, after = { tearDown() }) {
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
    val actualDataList = apolloClient.query(WithInlineFragmentsQuery()).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }
}
