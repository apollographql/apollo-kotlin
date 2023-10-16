package test

import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.mockServerTest
import com.example.Get1Query
import com.example.GetListQuery
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

class CcnTest {
  @Test
  fun testScalar() = mockServerTest {
    mockServer.enqueueString("""
      {
        "data": {
          "nullable": 41,
          "nonNullable": null
        }
      }
    """.trimIndent())

    val response = apolloClient.query(Get1Query()).execute()

    assertEquals(41, response.data!!.nullable)
    assertEquals(null, response.data!!.nonNullable)
  }

  @Test
  fun testList() = mockServerTest {
    @Language("JSON")
    val json = """
      {
        "data": {
          "user": {
            "friends": [
              null
            ],
            "enemies": [
              {
                "name": "nullability"
              }
            ],
            "frenemies": []
          }
        }
      }
    """.trimIndent()
    mockServer.enqueueString(json)

    val response = apolloClient.query(GetListQuery()).execute()

    if (response.exception != null) {
      response.exception?.printStackTrace()
    }
    assertEquals(null, response.data!!.user!!.friends[0]?.name)
    assertEquals("nullability", response.data!!.user!!.enemies[0].name)
    assertEquals(0, response.data!!.user!!.frenemies.size)
  }
}
