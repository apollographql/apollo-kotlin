package test

import IdCacheKeyGenerator
import IdCacheResolver
import assertEquals2
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
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
import com.apollographql.apollo3.testing.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every other test that doesn't fit in the other files
 */
@OptIn(ApolloExperimental::class)
class OtherCacheTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), cacheKeyGenerator = IdCacheKeyGenerator, cacheResolver = IdCacheResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun masterDetailSuccess() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Store a query that contains all data
    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE))
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()

    // Getting a subtree of that data should work
    val detailsResponse = apolloClient.query(CharacterNameByIdQuery("1002"))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()


    assertEquals(detailsResponse.data?.character!!.name, "Han Solo")
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailFailIncomplete() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Store a query that contains all data
    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE))
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()

    // Some details are not present in the master query, we should get a cache miss
    try {
      apolloClient.query(CharacterDetailsQuery("1002")).fetchPolicy(FetchPolicy.CacheOnly).execute()
      fail("we expected a cache miss")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("Object '1002' has no field named '__typename'"))
    }
  }


  @Test
  fun cacheMissThrows() = runTest(before = { setUp() }, after = { tearDown() }) {
    try {
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .execute()
      fail("we expected a cache miss")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("Object 'QUERY_ROOT' has no field named 'hero"))
    }
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirective() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameResponse.json"))
    apolloClient.query(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false))
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()

    var response = apolloClient.query(
        HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()

    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        HeroAndFriendsDirectivesQuery(Episode.JEDI, false, false))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()

    assertNull(response.data?.hero?.name)
    assertEquals2(response.data?.hero?.friends?.size, 3)
    assertEquals2(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals2(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals2(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    response = apolloClient.query(
        HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()

    assertEquals2(response.data?.hero?.name, "R2-D2")
    assertNull(response.data?.hero?.friends)
  }


  @Test
  fun skipIncludeDirectiveUnsatisfiedCache() = runTest(before = { setUp() }, after = { tearDown() }) {
    // Store a response that doesn't contain friends
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
    apolloClient.query(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true))
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()


    // Get it from the cache, we should get the name but no friends
    val response = apolloClient.query(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, true))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends, null)

    // Now try to get the friends from the cache, it should fail
    try {
      apolloClient.query(HeroAndFriendsDirectivesQuery(Episode.JEDI, true, false))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .execute()

      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
      assertTrue(e.message!!.contains("has no field named 'friends'"))
    }
  }
}