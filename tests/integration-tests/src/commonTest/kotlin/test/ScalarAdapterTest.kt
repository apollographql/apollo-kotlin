package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.integration.fullstack.LaunchDetailsByDateQuery
import com.apollographql.apollo3.integration.fullstack.type.Date
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.example.MyDate
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarAdapterTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  private object MyDateAdapter : ScalarAdapter<MyDate> {
    override fun fromJson(reader: JsonReader): MyDate {
      val elements = reader.nextString()!!.split('-').map { it.toInt() }
      return MyDate(elements[0], elements[1], elements[2])
    }

    override fun toJson(writer: JsonWriter, value: MyDate) {
      writer.value("${value.year}-${value.month}-${value.day}")
    }
  }

  @Test
  fun regularScalarAdapter() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("LaunchDetailsByDateResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addScalarAdapter(Date.type, MyDateAdapter)
        .build()

    val response = apolloClient.query(LaunchDetailsByDateQuery(MyDate(2001, 6, 23))).execute()

    val request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains(""""variables":{"date":"2001-6-23"}"""))

    assertEquals(MyDate(1978, 4, 27), response.dataOrThrow().launchByDate?.date)
  }
}
