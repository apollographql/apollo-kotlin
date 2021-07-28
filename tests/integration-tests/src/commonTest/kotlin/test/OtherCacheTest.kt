package test

import assertEquals2
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.cache.normalized.ApolloStore
import IdCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsDirectivesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import IdObjectIdGenerator
import com.apollographql.apollo3.cache.normalized.withFetchPolicy
import com.apollographql.apollo3.cache.normalized.withStore
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
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }

  @Test
  fun masterDetailSuccess() = runWithMainLoop {
    // Store a query that contains all data
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    // Getting a subtree of that data should work
    val detailsResponse = apolloClient.query(
        ApolloRequest(CharacterNameByIdQuery("1002")).withFetchPolicy(FetchPolicy.CacheOnly)
    )

    assertEquals(detailsResponse.data?.character!!.name, "Han Solo")
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailFailIncomplete() = runWithMainLoop {
    // Store a query that contains all data
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    // Some details are not present in the master query, we should get a cache miss
    try {
      apolloClient.query(
          ApolloRequest(CharacterDetailsQuery("1002")).withFetchPolicy(FetchPolicy.CacheOnly)
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
          ApolloRequest(EpisodeHeroNameQuery(Episode.EMPIRE)).withFetchPolicy(FetchPolicy.CacheOnly)
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
        ApolloRequest(HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = false))
            .withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    var response = apolloClient.query(
        ApolloRequest(HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = false))
            .withFetchPolicy(FetchPolicy.CacheOnly)
    )
    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        ApolloRequest(HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = false, skipFriends = false))
            .withFetchPolicy(FetchPolicy.CacheOnly)
    )
    assertNull(response.data?.hero?.name)
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        ApolloRequest(HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = true))
            .withFetchPolicy(FetchPolicy.CacheOnly)
    )
    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertNull(response.data?.hero?.friends)
  }


  @Test
  fun skipIncludeDirectiveUnsatisfiedCache() = runWithMainLoop {
    // Store a response that doesn't contain friends
    mockServer.enqueue(readResource("HeroNameResponse.json"))
    apolloClient.query(
        ApolloRequest(
            HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = true)
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    // Get it from the cache, we should get the name but no friends
    val response = apolloClient.query(
        ApolloRequest(
            HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = true)
        ).withFetchPolicy(FetchPolicy.CacheOnly)
    )

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends, null)

    // Now try to get the friends from the cache, it should fail
    try {
      apolloClient.query(
          ApolloRequest(
              HeroAndFriendsDirectivesQuery(episode = Episode.JEDI, includeName = true, skipFriends = false)
          ).withFetchPolicy(FetchPolicy.CacheOnly)
      )
      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("has no field named 'friends'"))
    }
  }
}