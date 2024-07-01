package testnetworktransport

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.enqueueTestNetworkError
import com.apollographql.apollo.testing.enqueueTestResponse
import com.apollographql.apollo.testing.internal.runTest
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import testnetworktransport.type.buildDroid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueTestNetworkTransportHandlerTest {
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
  }

  @Test
  fun enqueueResponses() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query1 = GetHeroQuery("001")
    val testResponse1 = ApolloResponse.Builder(
        operation = query1,
        requestUuid = uuid4(),
    ).errors(
        listOf(
            Error.Builder(message = "There was an error")
                .locations(listOf(Error.Location(line = 1, column = 2)))
                .path(listOf("hero", "name"))
                .build()
        )
    ).build()

    val query2 = GetHeroQuery("002")
    val query2TestData = GetHeroQuery.Data(
        GetHeroQuery.Hero(
            __typename = "Droid",
            name = "R2D2",
            id = "r2d2",
            onDroid = GetHeroQuery.OnDroid("mechadroid"),
            onHuman = null
        )
    )
    val testResponse2 = ApolloResponse.Builder(query2, uuid4()).data(query2TestData).build()

    val query3 = GetHeroNameOnlyQuery()
    val testResponse3 = ApolloResponse.Builder(query3, uuid4())
        .data(GetHeroNameOnlyQuery.Data(GetHeroNameOnlyQuery.Hero(name = "Darth Vader")))
        .build()

    apolloClient.apply {
      enqueueTestResponse(testResponse1)
      enqueueTestResponse(testResponse2)
      enqueueTestResponse(testResponse3)
    }

    val actual1: ApolloResponse<GetHeroQuery.Data> = apolloClient.query(query1).execute()
    assertTrue(actual1.hasErrors())
    assertEquals(testResponse1.errors!!.first().message, actual1.errors!!.first().message)

    val actual2: ApolloResponse<GetHeroQuery.Data> = apolloClient.query(query2).execute()
    assertFalse(actual2.hasErrors())
    assertEquals(query2TestData, actual2.data)

    val actual3: ApolloResponse<GetHeroNameOnlyQuery.Data> = apolloClient.query(query3).execute()
    assertFalse(actual3.hasErrors())
    assertEquals("Darth Vader", actual3.data!!.hero.name)
  }

  @Test
  fun enqueueError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery("001")
    apolloClient.enqueueTestResponse(
        operation = query,
        errors = listOf(
            Error.Builder(message = "There was an error")
                .locations(listOf(Error.Location(line = 1, column = 2)))
                .path(listOf("hero", "name"))
                .putExtension("myExtension", true)
                .build()
        )
    )

    val actual: ApolloResponse<GetHeroQuery.Data> = apolloClient.query(query).execute()
    assertTrue(actual.hasErrors())
    assertEquals(actual.errors!!.first().message, "There was an error")
    assertEquals(actual.errors!!.first().extensions!!["myExtension"], true)
  }

  @Test
  fun enqueueNetworkError() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient.enqueueTestNetworkError()

    apolloClient.query(GetHeroQuery("001")).toFlow()
        .test {
          assertTrue(awaitItem().exception?.message?.contains("Network error queued") ?: false)
          cancelAndConsumeRemainingEvents()
        }
  }

  @Test
  fun enqueueDataTestBuilder() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery("001")
    val testData = GetHeroQuery.Data {
      hero = buildDroid {
        name = "R2D2"
      }
    }
    apolloClient.enqueueTestResponse(query, testData)

    val actual = apolloClient.query(query).execute().data!!
    assertEquals(testData.hero.name, actual.hero.name)
  }

  @Test
  fun errorWhenNoResponseFoundInQueue() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient.query(GetHeroNameOnlyQuery()).toFlow().catch { e ->
      assertTrue(e is IllegalStateException)
      assertTrue(e.message!!.startsWith("No more responses in queue"))
    }.toList()
  }
}
