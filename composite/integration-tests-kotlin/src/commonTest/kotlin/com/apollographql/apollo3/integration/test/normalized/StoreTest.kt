package com.apollographql.apollo3.integration.test.normalized

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.IdFieldCacheKeyResolver
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsWithFragmentsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsWithFragmentsQuery.Data.Hero.Companion.heroWithFriendsFragment
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragment.Friend.Companion.humanWithIdFragment
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragmentImpl
import com.apollographql.apollo3.integration.normalizer.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.isFromCache
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Tests that write into the store programmatically.
 *
 * XXX: Do we need a client and mockServer for these tests?
 */
class StoreTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }

  @Test
  fun removeFromStore() = runWithMainLoop {
    storeAllFriends()
    assertFriendIsCached("1002", "Han Solo")

    // remove the root query object
    var removed = store.remove(CacheKey.from("2001"))
    assertEquals(true, removed)

    // Trying to get the full response should fail
    assertRootNotCached()

    // put everything in the cache
    storeAllFriends()
    assertFriendIsCached("1002", "Han Solo")

    // remove a single object from the list
    removed = store.remove(CacheKey.from("1002"))
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
  fun removeMultipleFromStore() = runWithMainLoop {
    storeAllFriends()
    assertFriendIsCached("1000", "Luke Skywalker")
    assertFriendIsCached("1002", "Han Solo")
    assertFriendIsCached("1003", "Leia Organa")

    // Now remove multiple keys
    val removed = store.remove(listOf(CacheKey.from("1002"), CacheKey.from("1000")))

    assertEquals(2, removed)

    // Trying to get the objects we just removed should fail
    assertFriendIsNotCached("1000")
    assertFriendIsNotCached("1002")
    assertFriendIsCached("1003", "Leia Organa")
  }


  @Test
  @Throws(Exception::class)
  fun cascadeRemove() = runWithMainLoop {
    // put everything in the cache
    storeAllFriends()

    assertFriendIsCached("1000", "Luke Skywalker")
    assertFriendIsCached("1002", "Han Solo")
    assertFriendIsCached("1003", "Leia Organa")

    // test remove root query object
    val removed = store.remove(CacheKey.from("2001"), true)
    assertEquals(true, removed)

    // Nothing should be cached anymore
    assertRootNotCached()
    assertFriendIsNotCached("1000")
    assertFriendIsNotCached("1002")
    assertFriendIsNotCached("1003")
  }

  @Test
  // This test currently fails because we don't store the typename in HeroAndFriendsNamesWithIDsQuery
  // So we can't query it from HeroWithFriendsFragment
  @Ignore
  fun readFragmentFromStore() = runWithMainLoop {
    mockServer.enqueue(readResource("HeroAndFriendsWithFragmentResponse.json"))
    apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.NEWHOPE)))

    val heroWithFriendsFragment = store.readFragment(
        HeroWithFriendsFragmentImpl(),
        CacheKey.from("2001"),
    )!!
    assertEquals(heroWithFriendsFragment.id, "2001")
    assertEquals(heroWithFriendsFragment.name, "R2-D2")
    assertEquals(heroWithFriendsFragment.friends?.size, 3)
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.humanWithIdFragment()?.id, "1000")
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.humanWithIdFragment()?.name, "Luke Skywalker")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.humanWithIdFragment()?.id, "1002")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.humanWithIdFragment()?.name, "Han Solo")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.humanWithIdFragment()?.id, "1003")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.humanWithIdFragment()?.name, "Leia Organa")

    var fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey.from("1000"),
    )!!

    assertEquals(fragment.id, "1000")
    assertEquals(fragment.name, "Luke Skywalker")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey.from("1002"),
    )!!
    assertEquals(fragment.id, "1002")
    assertEquals(fragment.name, "Han Solo")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey.from("1003"),
    )!!
    assertEquals(fragment.id, "1003")
    assertEquals(fragment.name, "Leia Organa")
  }

  /**
   * Modify the store by writing fragments
   */
  @Test
  fun fragments() = runWithMainLoop {
    mockServer.enqueue(readResource("HeroAndFriendsWithFragmentResponse.json"))
    val query = HeroAndFriendsWithFragmentsQuery(Input.Present(Episode.NEWHOPE))
    var response = apolloClient.query(query)
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.id, "2001")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.name, "R2-D2")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.size, 3)
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.__typename, "Human")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.id, "1000")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.name, "Luke Skywalker")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.__typename, "Human")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.id, "1002")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.name, "Han Solo")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(2)?.humanWithIdFragment()?.__typename, "Human")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(2)?.humanWithIdFragment()?.id, "1003")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(2)?.humanWithIdFragment()?.name, "Leia Organa")

    store.writeFragment(
        HeroWithFriendsFragmentImpl(),
        CacheKey.from("2001"),
        HeroWithFriendsFragmentImpl.Data(
            __typename = "Droid",
            id = "2001",
            name = "R222-D222",
            friends = listOf(
                HeroWithFriendsFragmentImpl.Data.HumanFriend(
                    __typename = "Human",
                    id = "1000",
                    name = "SuperMan"
                ),
                HeroWithFriendsFragmentImpl.Data.HumanFriend(
                    __typename = "Human",
                    id = "1002",
                    name = "Han Solo"
                ),
            )
        ),
    )

    store.writeFragment(
        HumanWithIdFragmentImpl(),
        CacheKey.from("1002"),
        HumanWithIdFragmentImpl.Data(
            __typename = "Human",
            id = "1002",
            name = "Beast"
        ),
    )

    // Values should have changed
    response = apolloClient.query(query)
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.id, "2001")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.name, "R222-D222")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.size, 2)
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.__typename, "Human")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.id, "1000")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.name, "SuperMan")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.__typename, "Human")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.id, "1002")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.name, "Beast")
  }

  private suspend fun storeAllFriends() {
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    val response = apolloClient.query(
        ApolloRequest(HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.NEWHOPE))).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends?.size, 3)
  }

  private suspend fun assertFriendIsCached(id: String, name: String) {
    val characterResponse = apolloClient.query(
        ApolloRequest(CharacterNameByIdQuery(id)).withFetchPolicy(FetchPolicy.CacheOnly)
    )
    assertEquals(characterResponse.isFromCache, true)
    assertEquals(characterResponse.data?.character?.name, name)
  }

  private suspend fun assertFriendIsNotCached(id: String) {
    try {
      apolloClient.query(
          ApolloRequest(CharacterNameByIdQuery(id)).withFetchPolicy(FetchPolicy.CacheOnly)
      )
      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
    }
  }

  private suspend fun assertRootNotCached() {
    try {
      apolloClient.query(
          ApolloRequest(HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.NEWHOPE))).withFetchPolicy(FetchPolicy.CacheOnly)
      )
      fail("A CacheMissException was expected")
    } catch (e: CacheMissException) {
    }
  }
}