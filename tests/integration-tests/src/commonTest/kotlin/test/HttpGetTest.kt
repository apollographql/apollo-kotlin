package test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.mockServerTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpGetTest {
  @Test
  fun gzipTest() = mockServerTest {
    mockServer.enqueue("""
      {
        "data": {
          "hero": {
            "__typename": "Droid",
            "name": "R2-D2",
            "friends": [
              {
                "__typename": "Human",
                "name": "Luke Skywalker"
              }
            ]
          }
        }
      }
    """.trimIndent())
    val response = apolloClient.query(HeroAndFriendsNamesQuery(Episode.JEDI))
        .httpMethod(HttpMethod.Get)
        .execute()
    assertEquals(response.data?.hero?.name, "R2-D2")
  }
}
