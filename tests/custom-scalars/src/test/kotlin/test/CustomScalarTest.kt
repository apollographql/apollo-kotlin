package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomTypeAdapter
import com.apollographql.apollo3.api.CustomTypeValue
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import custom.scalars.Address
import custom.scalars.GetAddressQuery
import custom.scalars.GetAllQuery
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomScalarTest {
  @Test
  fun adaptersDontNeedToBeRegistered() = runTest {
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
          "string": "string",
          "nullableString": null,
          "int": 1,
          "nullableInt": null,
          "boolean": true,
          "nullableBoolean": null,
          "notMapped": { "key": "value" },
          "nullableNotMapped": null
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = server.url()).build()
        .query(GetAllQuery())
        .execute()
        .dataAssertNoErrors
    assertEquals(1, data.id)
    assertNull(data.nullableId)
    assertEquals(10_000_000_000, data.long)
    assertEquals(1.4f, data.float)
    assertEquals(mapOf("key" to "value"), data.any)
    assertEquals(listOf(
        mapOf("lat" to 1, "lon" to 2),
        mapOf("lat" to 3, "lon" to 4),
    ), data.geoPoints)
    assertEquals("string", data.string)
    assertNull(data.nullableString)
    assertEquals(1, data.int)
    assertNull(data.nullableInt)
    assertEquals(true, data.boolean)
    assertNull(data.nullableBoolean)
    assertEquals(mapOf("key" to "value"), data.notMapped)
    assertNull(data.nullableNotMapped)
  }

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
        .query(GetAddressQuery())
        .execute()
        .dataAssertNoErrors

    assertEquals(Address("Downing Street", 10), data.address)
  }
}
