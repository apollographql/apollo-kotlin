package test

import IdObjectIdGenerator
import codegen.models.HeroAndFriendsWithFragmentsQuery
import codegen.models.HeroAndFriendsWithTypenameQuery
import codegen.models.fragment.HeroWithFriendsFragment
import codegen.models.fragment.HeroWithFriendsFragmentImpl
import codegen.models.fragment.HumanWithIdFragment
import codegen.models.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readJson
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        objectIdGenerator = IdObjectIdGenerator
    )
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  @Test
  fun readFragmentFromStore() = runWithMainLoop {
    mockServer.enqueue(readJson("HeroAndFriendsWithTypename.json"))
    apolloClient.query(HeroAndFriendsWithTypenameQuery())

    val heroWithFriendsFragment = store.readFragment(
        HeroWithFriendsFragmentImpl(),
        CacheKey("2001"),
    )!!
    assertEquals(heroWithFriendsFragment.id, "2001")
    assertEquals(heroWithFriendsFragment.name, "R2-D2")
    assertEquals(heroWithFriendsFragment.friends?.size, 3)
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.fragments?.humanWithIdFragment?.id, "1000")
    assertEquals(heroWithFriendsFragment.friends?.get(0)?.fragments?.humanWithIdFragment?.name, "Luke Skywalker")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.fragments?.humanWithIdFragment?.id, "1002")
    assertEquals(heroWithFriendsFragment.friends?.get(1)?.fragments?.humanWithIdFragment?.name, "Han Solo")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.fragments?.humanWithIdFragment?.id, "1003")
    assertEquals(heroWithFriendsFragment.friends?.get(2)?.fragments?.humanWithIdFragment?.name, "Leia Organa")

    var fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1000"),
    )!!

    assertEquals(fragment.id, "1000")
    assertEquals(fragment.name, "Luke Skywalker")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1002"),
    )!!
    assertEquals(fragment.id, "1002")
    assertEquals(fragment.name, "Han Solo")

    fragment = store.readFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1003"),
    )!!
    assertEquals(fragment.id, "1003")
    assertEquals(fragment.name, "Leia Organa")
  }

  /**
   * Modify the store by writing fragments
   */
  @Test
  fun fragments() = runWithMainLoop {
    mockServer.enqueue(readJson("HeroAndFriendsNamesWithIDs.json"))
    val query = HeroAndFriendsWithFragmentsQuery()
    var response = apolloClient.query(query)
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.id, "2001")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.name, "R2-D2")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.size, 3)
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(0)?.fragments?.humanWithIdFragment?.id, "1000")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(0)?.fragments?.humanWithIdFragment?.name, "Luke Skywalker")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(1)?.fragments?.humanWithIdFragment?.id, "1002")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(1)?.fragments?.humanWithIdFragment?.name, "Han Solo")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(2)?.fragments?.humanWithIdFragment?.id, "1003")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(2)?.fragments?.humanWithIdFragment?.name, "Leia Organa")

    store.writeFragment(
        HeroWithFriendsFragmentImpl(),
        CacheKey("2001"),
        HeroWithFriendsFragment(
            id = "2001",
            name = "R222-D222",
            friends = listOf(
                HeroWithFriendsFragment.Friend(
                    __typename = "Human",
                    fragments = HeroWithFriendsFragment.Friend.Fragments(
                        humanWithIdFragment = HumanWithIdFragment(
                            id = "1000",
                            name = "SuperMan"
                        )
                    )
                ),
                HeroWithFriendsFragment.Friend(
                    __typename = "Human",
                    fragments = HeroWithFriendsFragment.Friend.Fragments(
                        humanWithIdFragment = HumanWithIdFragment(
                            id = "1002",
                            name = "Han Solo"
                        )
                    )
                ),
            )
        ),
    )

    store.writeFragment(
        HumanWithIdFragmentImpl(),
        CacheKey("1002"),
        HumanWithIdFragment(
            id = "1002",
            name = "Beast"
        ),
    )

    // Values should have changed
    response = apolloClient.query(query)
    assertEquals(response.data?.hero?.__typename, "Droid")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.id, "2001")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.name, "R222-D222")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.size, 2)
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(0)?.fragments?.humanWithIdFragment?.id, "1000")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(0)?.fragments?.humanWithIdFragment?.name, "SuperMan")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(1)?.fragments?.humanWithIdFragment?.id, "1002")
    assertEquals(response.data?.hero?.fragments?.heroWithFriendsFragment?.friends?.get(1)?.fragments?.humanWithIdFragment?.name, "Beast")
  }
}