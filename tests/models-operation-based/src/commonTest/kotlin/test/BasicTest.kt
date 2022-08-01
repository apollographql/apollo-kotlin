package test

import IdCacheKeyGenerator
import codegen.models.HeroParentTypeDependentFieldQuery
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.type.Episode
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicTest {
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
    mockServer.stop()
  }

  private fun <D : Query.Data> basicTest(resourceName: String, query: Query<D>, block: ApolloResponse<D>.() -> Unit) = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8(resourceName))
    var response = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()
    response.block()
    response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()
    response.block()
  }

  @Test
  @Throws(Exception::class)
  fun heroParentTypeDependentField() = basicTest(
      "HeroParentTypeDependentField.json",
      HeroParentTypeDependentFieldQuery(Optional.Present(Episode.NEWHOPE))
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
  fun polymorphicDroidFieldsGetParsedToDroid() = basicTest(
      "MergedFieldWithSameShape_Droid.json",
      MergedFieldWithSameShapeQuery(Optional.Present(Episode.NEWHOPE))
  ) {
    assertFalse(hasErrors())
    assertTrue(data?.hero?.onDroid != null)
    assertEquals(data?.hero?.onDroid?.property, "Astromech")
  }

  @Test
  fun polymorphicHumanFieldsGetParsedToHuman() = basicTest(
      "MergedFieldWithSameShape_Human.json",
      MergedFieldWithSameShapeQuery(Optional.Present(Episode.NEWHOPE))
  ) {
    assertFalse(hasErrors())
    assertTrue(data?.hero?.onHuman != null)
    assertEquals(data?.hero?.onHuman?.property, "Tatooine")
  }
}
