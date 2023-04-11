package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyScalarAdapter
import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import custom.scalars.Address
import custom.scalars.AddressQuery
import custom.scalars.BuiltInAdaptersQuery
import custom.scalars.CompileTimeAdaptersQuery
import custom.scalars.DecimalQuery
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomScalarTest {
  /**
   * scalar types that are going to reuse the built in adapters
   */
  @Test
  fun builtInAdapters() = runTest {
    val server = MockServer()
    server.enqueue("""
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

    val data = ApolloClient.Builder().serverUrl(serverUrl = server.url()).build()
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
    val server = MockServer()
    server.enqueue("""
      {
        "data": {
          "int": 1,
          "nullableInt": null,
          "string": "string",
          "nullableString": null
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = server.url()).build()
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
    val server = MockServer()
    server.enqueue("""
      {
        "data": {
          "decimal": 1000000000000000000000000000000000000000000
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder()
        .serverUrl(serverUrl = server.url())
        .build()
        .query(DecimalQuery())
        .execute()
        .dataOrThrow()

    assertEquals("1000000000000000000000000000000000000000000", data.decimal?.toString())
  }

  /**
   * Test the backward compat `addCustomTypeAdapter`
   */
  @Test
  fun addCustomTypeAdapter() = runTest {
    val server = MockServer()
    server.enqueue("""
      {
        "data": {
          "address": {
            "street": "Downing Street",
            "number": 10
          }
        }
      }
    """.trimIndent())

    val customTypeAdapter = object : ScalarAdapter<Address> {
      override fun fromJson(reader: JsonReader): Address {
        @Suppress("UNCHECKED_CAST")
        val map = AnyScalarAdapter.fromJson(reader) as Map<String, Any?>

        return Address(map.get("street") as String, map.get("number") as Int)
      }

      override fun toJson(writer: JsonWriter, value: Address) {
        writer.writeObject {
          name("street")
          value(value.street)
          name("number")
          value(value.number)
        }
      }
    }
    val data = ApolloClient.Builder()
        .serverUrl(serverUrl = server.url())
        .addScalarAdapter(custom.scalars.type.Address.type, customTypeAdapter)
        .build()
        .query(AddressQuery())
        .execute()
        .dataOrThrow()

    assertEquals(Address("Downing Street", 10), data.address)
  }
}
