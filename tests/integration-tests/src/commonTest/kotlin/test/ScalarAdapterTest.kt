@file:Suppress("DEPRECATION")

package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.CustomTypeAdapter
import com.apollographql.apollo3.api.CustomTypeValue
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.integration.fullstack.LaunchDetailsByDateQuery
import com.apollographql.apollo3.integration.fullstack.type.Date
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import com.example.MyDate
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class ScalarAdapterTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
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

  private object MyDateV2CustomTypeAdapter : CustomTypeAdapter<MyDate> {
    override fun decode(value: CustomTypeValue<*>): MyDate {
      val elements = value.value.toString().split('-').map { it.toInt() }
      return MyDate(elements[0], elements[1], elements[2])
    }

    override fun encode(value: MyDate): CustomTypeValue<*> {
      return CustomTypeValue.fromRawValue("${value.year}-${value.month}-${value.day}")
    }
  }

  @Test
  fun regularCustomScalarAdapter() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("LaunchDetailsByDateResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addCustomScalarAdapter(Date.type, MyDateAdapter)
        .build()

    val response = apolloClient.query(LaunchDetailsByDateQuery(MyDate(2001, 6, 23))).execute()

    val request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains(""""variables":{"date":"2001-6-23"}"""))

    assertEquals(MyDate(1978, 4, 27), response.dataAssertNoErrors.launchByDate?.date)
  }

  @Test
  fun version2CustomTypeAdapter() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("LaunchDetailsByDateResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addCustomTypeAdapter(Date.type, MyDateV2CustomTypeAdapter)
        .build()

    val response = apolloClient.query(LaunchDetailsByDateQuery(MyDate(2001, 6, 23))).execute()

    val request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains(""""variables":{"date":"2001-6-23"}"""))

    assertEquals(MyDate(1978, 4, 27), response.dataAssertNoErrors.launchByDate?.date)
  }
}
