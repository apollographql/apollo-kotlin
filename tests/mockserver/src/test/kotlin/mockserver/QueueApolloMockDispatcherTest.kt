package mockserver

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.MockServer
import com.apollographql.apollo3.testing.QueueApolloMockDispatcher
import com.apollographql.apollo3.testing.runTest
import com.benasher44.uuid.uuid4
import mockserver.test.GetHeroQuery_TestBuilder.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class QueueApolloMockDispatcherTest {
  private lateinit var dispatcher: QueueApolloMockDispatcher
  private lateinit var mockServer: MockServer

  private fun setUp() {
    dispatcher = QueueApolloMockDispatcher()
    mockServer = MockServer(dispatcher)
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun enqueueResponses() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query1 = GetHeroQuery()
    val testResponse1 = ApolloResponse.Builder(query1, uuid4(), null)
        .errors(listOf(Error(
            message = "There was an error",
            locations = listOf(Error.Location(line = 1, column = 2)),
            path = listOf("hero", "name"),
            extensions = null,
            nonStandardFields = null))
        ).build()

    val query2 = GetHeroNameOnlyQuery()
    val testResponse2 = ApolloResponse.Builder(query2, uuid4(), GetHeroNameOnlyQuery.Data(GetHeroNameOnlyQuery.Hero(name = "Darth Vader")))
        .build()

    dispatcher.enqueue(testResponse1)
    dispatcher.enqueue(testResponse2)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual1: ApolloResponse<GetHeroQuery.Data> = apolloClient.query(query1).execute()
    assertTrue(actual1.hasErrors())
    assertEquals(testResponse1.errors!!.first().message, actual1.errors!!.first().message)

    val actual2: ApolloResponse<GetHeroNameOnlyQuery.Data> = apolloClient.query(query2).execute()
    assertFalse(actual2.hasErrors())
  }

  @Test
  fun enqueueError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery()
    dispatcher.enqueue(query, errors = listOf(Error(
        message = "There was an error",
        locations = listOf(Error.Location(line = 1, column = 2)),
        path = listOf("hero", "name"),
        extensions = mapOf("myExtension" to true),
        nonStandardFields = null))
    )

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual: ApolloResponse<GetHeroQuery.Data> = apolloClient.query(query).execute()
    assertTrue(actual.hasErrors())
    assertEquals(actual.errors!!.first().message, "There was an error")
    assertEquals(actual.errors!!.first().extensions!!["myExtension"], true)
  }

  @Test
  fun enqueueDataManual() = runTest(before = { setUp() }, after = { tearDown() }) {
    val dispatcher = QueueApolloMockDispatcher()
    mockServer = MockServer(dispatcher)

    val query = GetHeroQuery()
    val testData = GetHeroQuery.Data(
        GetHeroQuery.Hero(
            __typename = "Droid",
            name = "R2D2",
            id = "r2d2",
            onDroid = GetHeroQuery.OnDroid("mechadroid"),
            onHuman = null
        )
    )
    dispatcher.enqueue(query, testData)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual = apolloClient.query(query).execute().data
    assertEquals(testData, actual)
  }

  @Test
  fun enqueueDataTestBuilder() = runTest(before = { setUp() }, after = { tearDown() }) {
    val dispatcher = QueueApolloMockDispatcher()
    mockServer = MockServer(dispatcher)

    val query = GetHeroQuery()
    val testData = GetHeroQuery.Data {
      hero = droidHero {
        name = "R2D2"
      }
    }
    dispatcher.enqueue(query, testData)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual = apolloClient.query(query).execute().data!!
    assertEquals(testData.hero.name, actual.hero.name)
  }
}
