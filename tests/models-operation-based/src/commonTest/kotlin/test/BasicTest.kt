package test

import IdObjectIdGenerator
import codegen.models.HeroParentTypeDependentFieldQuery
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.type.Episode
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.withFetchPolicy
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readJson
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicTest {
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

  private fun <D : Query.Data> basicTest(resourceName: String, query: Query<D>, block: ApolloResponse<D>.() -> Unit) = runWithMainLoop {
    mockServer.enqueue(readJson(resourceName))
    var response = apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))
    response.block()
    response = apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))
    response.block()
  }

  @Test
  @Throws(Exception::class)
  fun heroParentTypeDependentField() = basicTest(
      "HeroParentTypeDependentField.json",
      HeroParentTypeDependentFieldQuery(Episode.NEWHOPE)
  ) {

    assertFalse(hasErrors())
    assertEquals(data?.hero?.name, "R2-D2")
    assertEquals(data?.hero?.name, "R2-D2")
    val hero = data?.hero?.onDroid!!
    assertEquals(hero.friends?.size, 3)
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals((hero.friends?.get(0)?.onHuman)?.height, 1.72)
  }


  @Test
  fun `polymorphic Droid fields get parsed to Droid`() = basicTest(
      "MergedFieldWithSameShape_Droid.json",
      MergedFieldWithSameShapeQuery(Episode.NEWHOPE)
  ) {
    assertFalse(hasErrors())
    assertTrue(data?.hero?.onDroid != null)
    assertEquals(data?.hero?.onDroid?.property, "Astromech")
  }

  @Test
  fun `polymorphic Human fields get parsed to Human`() = basicTest(
      "MergedFieldWithSameShape_Human.json",
      MergedFieldWithSameShapeQuery(Episode.NEWHOPE)
  ) {
    assertFalse(hasErrors())
    assertTrue(data?.hero?.onHuman != null)
    assertEquals(data?.hero?.onHuman?.property, "Tatooine")
  }
}