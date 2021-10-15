package test

import IdCacheResolver
import IdObjectIdGenerator
import assertEquals2
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsDirectivesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readResource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every other test that doesn't fit in the other files
 */
class OtherCacheTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), objectIdGenerator = IdObjectIdGenerator, cacheResolver = IdCacheResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  @Test
  fun masterDetailSuccess() = runWithMainLoop {
    // Store a query that contains all data
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).fetchPolicy(FetchPolicy.NetworkOnly).build()
    )

    // Getting a subtree of that data should work
    val detailsResponse = apolloClient.query(
        ApolloRequest.Builder(CharacterNameByIdQuery("1002")).fetchPolicy(FetchPolicy.CacheOnly).build()
    )

    assertEquals(detailsResponse.data?.character!!.name, "Han Solo")
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailFailIncomplete() = runWithMainLoop {
    // Store a query that contains all data
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).fetchPolicy(FetchPolicy.NetworkOnly).build()
    )

    // Some details are not present in the master query, we should get a cache miss
    try {
      apolloClient.query(
          ApolloRequest.Builder(CharacterDetailsQuery("1002")).fetchPolicy(FetchPolicy.CacheOnly).build()
      )
      fail("we expected a cache miss")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("Object '1002' has no field named '__typename'"))
    }
  }


  @Test
  fun cacheMissThrows() = runWithMainLoop {
    try {
      apolloClient.query(
          ApolloRequest.Builder(EpisodeHeroNameQuery(Episode.EMPIRE)).fetchPolicy(FetchPolicy.CacheOnly).build()
      )
      fail("we expected a cache miss")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("Object 'QUERY_ROOT' has no field named 'hero"))
    }
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirective() = runWithMainLoop {
    mockServer.enqueue(readResource("HeroAndFriendsNameResponse.json"))
    apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .build()
    )

    var response = apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false))
            .fetchPolicy(FetchPolicy.CacheOnly)
            .build()
    )
    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsDirectivesQuery(Episode.JEDI, false, false))
            .fetchPolicy(FetchPolicy.CacheOnly)
            .build()
    )
    assertNull(response.data?.hero?.name)
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        ApolloRequest.Builder(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true))
            .fetchPolicy(FetchPolicy.CacheOnly)
            .build()
    )
    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertNull(response.data?.hero?.friends)
  }


  @Test
  fun skipIncludeDirectiveUnsatisfiedCache() = runWithMainLoop {
    // Store a response that doesn't contain friends
    mockServer.enqueue(readResource("HeroNameResponse.json"))
    apolloClient.query(
        ApolloRequest.Builder(
            HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true)
        ).fetchPolicy(FetchPolicy.NetworkOnly)
            .build()
    )

    // Get it from the cache, we should get the name but no friends
    val response = apolloClient.query(
        ApolloRequest.Builder(
            HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true)
        ).fetchPolicy(FetchPolicy.CacheOnly)
            .build()
    )

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends, null)

    // Now try to get the friends from the cache, it should fail
    try {
      apolloClient.query(
          ApolloRequest.Builder(
              HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false)
          ).fetchPolicy(FetchPolicy.CacheOnly)
              .build()
      )
      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("has no field named 'friends'"))
    }
  }
}