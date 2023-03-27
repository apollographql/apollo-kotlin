package test

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.SearchHeroQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueueData
import com.apollographql.apollo3.testing.mockServerTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpGetTest {
  @Test
  fun getTest() = mockServerTest {
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
  @Test
  fun encodeReservedCharactersTest() = mockServerTest {
    // Response not needed, just testing generated url
    mockServer.enqueueData(data = emptyMap())
    apolloClient.query(SearchHeroQuery("!#$&'()*+,/:;=?@[]{}% "))
            .httpMethod(HttpMethod.Get)
            .execute()
    assertEquals(
            "/?operationName=SearchHero&variables=%7B%22text%22%3A%22%21%23%24%26%27%28%29%2A%2B%2C%2F%3A%3B%3D%3F%40%5B%5D%7B%7D%25%20%22%7D&query=query%20SearchHero%28%24text%3A%20String%29%20%7B%20search%28text%3A%20%24text%29%20%7B%20__typename%20...%20on%20Character%20%7B%20__typename%20name%20...%20on%20Human%20%7B%20homePlanet%20%7D%20...%20on%20Droid%20%7B%20primaryFunction%20%7D%20%7D%20%7D%20%7D",
            mockServer.takeRequest().path
    )
  }
}
