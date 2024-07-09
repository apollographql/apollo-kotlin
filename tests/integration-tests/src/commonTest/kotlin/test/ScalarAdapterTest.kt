package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.integration.fullstack.LaunchDetailsByDateQuery
import com.apollographql.apollo.integration.fullstack.type.Date
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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
    mockServer.close()
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

  @Test
  fun regularCustomScalarAdapter() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("LaunchDetailsByDateResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addCustomScalarAdapter(Date.type, MyDateAdapter)
        .build()

    val response = apolloClient.query(LaunchDetailsByDateQuery(MyDate(2001, 6, 23))).execute()

    val request = mockServer.awaitRequest()
    assertTrue(request.body.utf8().contains(""""variables":{"date":"2001-6-23"}"""))

    assertEquals(MyDate(1978, 4, 27), response.dataOrThrow().launchByDate?.date)
  }
}
