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
    assertEquals(
        "/?operationName=HeroAndFriendsNames&variables=%7B%22episode%22%3A%22JEDI%22%7D&query=query%20HeroAndFriendsNames%28%24episode%3A%20Episode%29%20%7B%20hero%28episode%3A%20%24episode%29%20%7B%20name%20friends%20%7B%20name%20%7D%20%7D%20%7D",
        mockServer.takeRequest().path
    )
  }
}
