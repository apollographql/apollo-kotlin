package test

import IdCacheKeyGenerator
import codegen.models.HeroAndFriendsWithFragmentsQuery
import codegen.models.HeroAndFriendsWithFragmentsQuery.Data.Hero.Companion.heroWithFriendsFragment
import codegen.models.HeroAndFriendsWithTypenameQuery
import codegen.models.fragment.HeroWithFriendsFragment.Friend.Companion.asHuman
import codegen.models.fragment.HeroWithFriendsFragment.Friend.Companion.humanWithIdFragment
import codegen.models.fragment.HeroWithFriendsFragmentImpl
import codegen.models.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        cacheKeyGenerator = IdCacheKeyGenerator
    )
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  @Test
  fun readFragmentFromStore() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("HeroAndFriendsWithTypename.json"))
    apolloClient.query(HeroAndFriendsWithTypenameQuery()).execute()

    val heroWithFriendsFragment = store.readFragment(
        HeroWithFriendsFragmentImpl(),
        CacheKey("2001"),
    )
    assertEquals(heroWithFriendsFragment.id, "2001")
    assertEquals(heroWithFriendsFragment.name, "R2-D2")
    assertEquals(heroWithFriendsFragment.friends?.size, 3)
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.asHuman()?.id, "1000")
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.asHuman()?.name, "Luke Skywalker")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.asHuman()?.id, "1002")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.asHuman()?.name, "Han Solo")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.asHuman()?.id, "1003")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.asHuman()?.name, "Leia Organa")

    var fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1000"),
    )

    assertEquals(fragment.id, "1000")
    assertEquals(fragment.name, "Luke Skywalker")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1002"),
    )
    assertEquals(fragment.id, "1002")
    assertEquals(fragment.name, "Han Solo")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1003"),
    )
    assertEquals(fragment.id, "1003")
    assertEquals(fragment.name, "Leia Organa")
  }

  /**
   * Modify the store by writing fragments
   */
  @Test
  fun fragments() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("HeroAndFriendsNamesWithIDs.json"))
    val query = HeroAndFriendsWithFragmentsQuery()
    var response = apolloClient.query(query).execute()
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.id, "2001")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.name, "R2-D2")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.size, 3)
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.id, "1000")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.name, "Luke Skywalker")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.id, "1002")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.name, "Han Solo")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(2)?.humanWithIdFragment()?.id, "1003")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(2)?.humanWithIdFragment()?.name, "Leia Organa")

    store.writeFragmentSync(
        HeroWithFriendsFragmentImpl(),
        CacheKey("2001"),
        HeroWithFriendsFragmentImpl.Data(
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

    store.writeFragmentSync(
        HumanWithIdFragmentImpl(),
        CacheKey("1002"),
        HumanWithIdFragmentImpl.Data(
            id = "1002",
            name = "Beast"
        ),
    )

    // Values should have changed
    response = apolloClient.query(query).execute()
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.id, "2001")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.name, "R222-D222")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.size, 2)
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.id, "1000")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(0)?.humanWithIdFragment()?.name, "SuperMan")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.id, "1002")
    assertEquals(response.data?.hero?.heroWithFriendsFragment()?.friends?.get(1)?.humanWithIdFragment()?.name, "Beast")
  }
}
