package test.outofbounds

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import custom.scalars.GetAllQuery
import org.junit.Test
import kotlin.test.assertEquals

class CustomScalarTest {
  @Test
  fun adaptersDontNeedToBeRegistered() = runTest{
    val server = MockServer()
    server.enqueue("""
      {
        "data": {
          "long": 10000000000,
          "float": 1.4,
          "any": { "key": "value" },
          "geoPoints": [
            { "lat": 1, "lon": 2 },
            { "lat": 3, "lon": 4 }
          ]
        }
      }
    """.trimIndent())

    val data = ApolloClient.Builder().serverUrl(serverUrl = server.url()).build()
        .query(GetAllQuery())
        .execute()
        .dataAssertNoErrors

    assertEquals(10_000_000_000, data.long_)
    assertEquals(1.4f, data.float_)
    assertEquals(mapOf("key" to "value"), data.any)
    assertEquals(listOf(
        mapOf("lat" to 1, "lon" to 2),
        mapOf("lat" to 3, "lon" to 4),
    ), data.geoPoints)
  }
}