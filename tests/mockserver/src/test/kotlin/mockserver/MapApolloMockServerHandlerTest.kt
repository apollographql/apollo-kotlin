package mockserver

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.MapApolloMockServerHandler
import com.apollographql.apollo3.testing.MockServer
import com.apollographql.apollo3.testing.runTest
import com.benasher44.uuid.uuid4
import mockserver.test.GetDateQuery_TestBuilder.Data
import mockserver.test.GetHeroQuery_TestBuilder.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class MapApolloMockServerHandlerTest {
  private lateinit var handler: MapApolloMockServerHandler
  private lateinit var mockServer: MockServer

  private fun setUp() {
    handler = MapApolloMockServerHandler()
    mockServer = MockServer(handler)
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun registerResponses() = runTest(before = { setUp() }, after = { tearDown() }) {
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

    handler.register(query1, testResponse1)
    handler.register(query2, testResponse2)

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
  fun registerError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery()
    handler.register(query, errors = listOf(Error(
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
  fun registerDataManual() = runTest(before = { setUp() }, after = { tearDown() }) {
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
    handler.register(query, testData)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual = apolloClient.query(query).execute().data
    assertEquals(testData, actual)
  }

  @Test
  fun registerDataTestBuilder() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery()
    val testData = GetHeroQuery.Data {
      hero = droidHero {
        name = "R2D2"
      }
    }
    handler.register(query, testData)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    val actual = apolloClient.query(query).execute().data!!
    assertEquals(testData.hero.name, actual.hero.name)
  }

  @Test
  fun customScalarAdapters() = runTest(after = { tearDown() }) {
    val customScalarAdapters = CustomScalarAdapters.Builder().add(mockserver.type.MyDate.type, MyDateAdapter).build()

    val dispatcher = MapApolloMockServerHandler(customScalarAdapters)
    mockServer = MockServer(dispatcher)

    val query = GetDateQuery()
    val testData = GetDateQuery.Data(customScalarAdapters = customScalarAdapters) {
      date = "1978-04-27"
    }
    dispatcher.register(query, testData)

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .customScalarAdapters(customScalarAdapters)
        .build()

    val actual = apolloClient.query(query).execute().data!!
    assertEquals(testData.date, actual.date)
  }

  private object MyDateAdapter : Adapter<MyDate> {
    override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): MyDate {
      val elements = reader.nextString()!!.split('-').map { it.toInt() }
      return MyDate(elements[0], elements[1], elements[2])
    }

    override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: MyDate) {
      writer.value("${value.year}-${value.month}-${value.day}")
    }
  }
}
