package test

import IdCacheKeyGenerator
import IdCacheResolver
import assertEquals2
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Tests that write into the store programmatically.
 *
 * XXX: Do we need a client and mockServer for these tests?
 */
@OptIn(ApolloExperimental::class)
class StoreTest {
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
  fun removeFromStore() = runTest(before = { setUp() }, after = { tearDown() }) {
    storeAllFriends()
    assertFriendIsCached("1002", "Han Solo")

    // remove the root query object
    var removed = store.remove(CacheKey("2001"))
    assertEquals(true, removed)

    // Trying to get the full response should fail
    assertRootNotCached()

    // put everything in the cache
    storeAllFriends()
    assertFriendIsCached("1002", "Han Solo")

    // remove a single object from the list
    removed = store.remove(CacheKey("1002"))
    assertEquals(true, removed)

    // Trying to get the full response should fail
    assertRootNotCached()

    // Trying to get the object we just removed should fail
    assertFriendIsNotCached("1002")

    // Trying to get another object we did not remove should work
    assertFriendIsCached("1003", "Leia Organa")
  }

  @Test
  @Throws(Exception::class)
  fun removeMultipleFromStore() = runTest(before = { setUp() }, after = { tearDown() }) {
    storeAllFriends()
    assertFriendIsCached("1000", "Luke Skywalker")
    assertFriendIsCached("1002", "Han Solo")
    assertFriendIsCached("1003", "Leia Organa")

    // Now remove multiple keys
    val removed = store.remove(listOf(CacheKey("1002"), CacheKey("1000")))

    assertEquals(2, removed)

    // Trying to get the objects we just removed should fail
    assertFriendIsNotCached("1000")
    assertFriendIsNotCached("1002")
    assertFriendIsCached("1003", "Leia Organa")
  }

  @Test
  @Throws(Exception::class)
  fun cascadeRemove() = runTest(before = { setUp() }, after = { tearDown() }) {
    // put everything in the cache
    storeAllFriends()

    assertFriendIsCached("1000", "Luke Skywalker")
    assertFriendIsCached("1002", "Han Solo")
    assertFriendIsCached("1003", "Leia Organa")

    // test remove root query object
    val removed = store.remove(CacheKey("2001"), true)
    assertEquals(true, removed)

    // Nothing should be cached anymore
    assertRootNotCached()
    assertFriendIsNotCached("1000")
    assertFriendIsNotCached("1002")
    assertFriendIsNotCached("1003")
  }

  @Test
  @Throws(Exception::class)
  fun directAccess() = runTest(before = { setUp() }, after = { tearDown() }) {
    // put everything in the cache
    storeAllFriends()

    store.accessCache {
      it.remove("10%")
    }
    assertFriendIsNotCached("1000")
    assertFriendIsNotCached("1002")
    assertFriendIsNotCached("1003")
  }

  private suspend fun storeAllFriends() {
    mockServer.enqueue(testFixtureToUtf8("HeroAndFriendsNameWithIdsResponse.json"))
    val response = apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE))
        .fetchPolicy(FetchPolicy.NetworkOnly).execute()

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends?.size, 3)
  }

  private suspend fun assertFriendIsCached(id: String, name: String) {
    val characterResponse = apolloClient.query(CharacterNameByIdQuery(id))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()

    assertEquals2(characterResponse.isFromCache, true)
    assertEquals2(characterResponse.data?.character?.name, name)
  }

  private suspend fun assertFriendIsNotCached(id: String) {
    try {
      apolloClient.query(CharacterNameByIdQuery(id))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .execute()

      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
    }
  }

  private suspend fun assertRootNotCached() {
    try {
      apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .execute()

      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
    }
  }
}