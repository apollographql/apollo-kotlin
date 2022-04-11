package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.adapter.BigDecimalAdapter
import com.apollographql.apollo3.adapter.toNumber
import com.apollographql.apollo3.api.CustomTypeAdapter
import com.apollographql.apollo3.api.CustomTypeValue
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import custom.scalars.Address
import custom.scalars.AddressQuery
import custom.scalars.BuiltInAdaptersQuery
import custom.scalars.CompileTimeAdaptersQuery
import custom.scalars.DecimalQuery
import custom.scalars.type.Decimal
import org.junit.Test
import java.math.BigDecimal
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
        .dataAssertNoErrors
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
          "nullableString": null,
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = server.url()).build()
        .query(CompileTimeAdaptersQuery())
        .execute()
        .dataAssertNoErrors

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
        .dataAssertNoErrors

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

    val customTypeAdapter = object : CustomTypeAdapter<Address> {
      override fun decode(value: CustomTypeValue<*>): Address {
        check(value is CustomTypeValue.GraphQLJsonObject)

        /**
         * XXX: For consistency, a [CustomTypeValue.GraphQLJsonObject] should contain `GraphQLFoo`
         * but in 2.x it contains primitive instead so keep that behaviour
         */
        val street = value.value["street"] as String
        val number = value.value["number"] as Int
        return Address(street, number)
      }

      override fun encode(value: Address): CustomTypeValue<*> {
        return CustomTypeValue.GraphQLJsonObject(mapOf(
            "street" to value.street,
            "number" to value.number
        ))
      }
    }
    val data = ApolloClient.Builder()
        .serverUrl(serverUrl = server.url())
        .addCustomTypeAdapter(custom.scalars.type.Address.type, customTypeAdapter)
        .build()
        .query(AddressQuery())
        .execute()
        .dataAssertNoErrors

    assertEquals(Address("Downing Street", 10), data.address)
  }
}
