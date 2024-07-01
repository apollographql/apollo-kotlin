package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.AnyAdapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.api.toJsonString
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import custom.scalars.Address
import custom.scalars.AddressQuery
import custom.scalars.BuiltInAdaptersQuery
import custom.scalars.CompileTimeAdaptersQuery
import custom.scalars.DecimalQuery
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomScalarTest {
  /**
   * scalar types that are going to reuse the built in adapters
   */
  @Test
  fun builtInAdapters() = runTest {
    val mockServer = MockServer()
    mockServer.enqueueString("""
      {
        "data": {
          "id": "1",
          "nullableId": null,
          "long": 10000000000,
          "float": 1.4,
          "any": { "key": "value" },
          "geoPoints": [
            { "lat": 1, "lon": 2 },
            { "lat": 3, "lon": 4 }
          ],
          "boolean": true,
          "nullableBoolean": null,
          "notMapped": { "key": "value" },
          "nullableNotMapped": null
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = mockServer.url()).build()
        .query(BuiltInAdaptersQuery())
        .execute()
        .dataOrThrow()
    assertEquals(1L, data.id)
    assertNull(data.nullableId)
    assertEquals(10_000_000_000L, data.long)
    assertEquals(1.4f, data.float)
    assertEquals(mapOf("key" to "value"), data.any)
    assertEquals(listOf(
        mapOf("lat" to 1, "lon" to 2),
        mapOf("lat" to 3, "lon" to 4),
    ), data.geoPoints)
    assertEquals(true, data.boolean)
    assertNull(data.nullableBoolean)
    assertEquals(mapOf("key" to "value"), data.notMapped)
    assertNull(data.nullableNotMapped)
  }

  /**
   * compileTime
   */
  @Test
  fun compileTimeAdapters() = runTest {
    val mockServer = MockServer()
    mockServer.enqueueString("""
      {
        "data": {
          "int": 1,
          "nullableInt": null,
          "string": "string",
          "nullableString": null
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = mockServer.url()).build()
        .query(CompileTimeAdaptersQuery())
        .execute()
        .dataOrThrow()

    assertEquals("string", data.string.value)
    assertNull(data.nullableString)
    assertEquals(1, data.int.value)
    assertNull(data.nullableInt)

  }

  @Test
  fun bigDecimal() = runTest {
    val mockServer = MockServer()
    mockServer.enqueueString("""
      {
        "data": {
          "decimal": 1000000000000000000000000000000000000000000
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder()
        .serverUrl(serverUrl = mockServer.url())
        .build()
        .query(DecimalQuery())
        .execute()
        .dataOrThrow()

    /*
     * Decimal is mapped to a String to get the same `toString` representation, else Double
     */
    assertEquals("1000000000000000000000000000000000000000000", data.decimal?.toString())
  }

  /**
   * Test the backward compat `addCustomTypeAdapter`
   */
  @Test
  fun addCustomTypeAdapter() = runTest {
    val mockServer = MockServer()
    mockServer.enqueueString("""
      {
        "data": {
          "address": {
            "street": "Downing Street",
            "number": 10
          }
        }
      }
    """.trimIndent())

    val customTypeAdapter = object : Adapter<Address> {
      override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Address {
        @Suppress("UNCHECKED_CAST")
        val map = AnyAdapter.fromJson(reader, CustomScalarAdapters.Empty) as Map<String, Any?>

        return Address(map.get("street") as String, map.get("number") as Int)
      }

      override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Address) {
        writer.writeObject {
          name("street")
          value(value.street)
          name("number")
          value(value.number)
        }
      }
    }
    val data = ApolloClient.Builder()
        .serverUrl(serverUrl = mockServer.url())
        .addCustomScalarAdapter(custom.scalars.type.Address.type, customTypeAdapter)
        .build()
        .query(AddressQuery())
        .execute()
        .dataOrThrow()

    assertEquals(Address("Downing Street", 10), data.address)
  }

  @Test
  fun forgettingToAddARuntimeAdapterForAScalarRegisteredInThePluginFails() {
    val dataString = """
      {
        "address": {
          "street": "Downing Street",
          "number": 10
        }
      }
    """.trimIndent()


    try {
      val query = AddressQuery()
      query.adapter().fromJson(Buffer().writeUtf8(dataString).jsonReader(), CustomScalarAdapters.Empty)
      error("expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("Can't map GraphQL type: `Address`"))
    }
  }
}
