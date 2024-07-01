package test

import IdCacheKeyGenerator
import codegen.models.HeroHumanOrDroidQuery
import codegen.models.HeroParentTypeDependentFieldQuery
import codegen.models.HeroParentTypeDependentFieldQuery.Data.DroidHero.Friend.Companion.asHuman
import codegen.models.HeroParentTypeDependentFieldQuery.Data.Hero.Companion.asDroid
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.MergedFieldWithSameShapeQuery.Data.Hero.Companion.asDroid
import codegen.models.MergedFieldWithSameShapeQuery.Data.Hero.Companion.asHuman
import codegen.models.type.Episode
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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
    mockServer.close()
  }

  private fun <D : Query.Data> basicTest(resourceName: String, query: Query<D>, block: ApolloResponse<D>.() -> Unit) = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8(resourceName))
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
    val hero = data?.hero?.asDroid()!!
    assertEquals(hero.friends?.size, 3)
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals((hero.friends?.get(0)?.asHuman())?.height, 1.72)
  }


  @Test
  fun polymorphicDroidFieldsGetParsedToDroid() = basicTest(
      "MergedFieldWithSameShape_Droid.json",
      MergedFieldWithSameShapeQuery(Optional.Present(Episode.NEWHOPE))
  ) {

    assertFalse(hasErrors())
    assertTrue(data?.hero is MergedFieldWithSameShapeQuery.Data.DroidHero)
    assertEquals(data?.hero?.asDroid()?.property, "Astromech")
  }

  @Test
  fun polymorphicHumanFieldsGetParsedToHuman() = basicTest(
      "MergedFieldWithSameShape_Human.json",
      MergedFieldWithSameShapeQuery(Optional.Present(Episode.NEWHOPE))
  ) {

    assertFalse(hasErrors())
    assertTrue(data?.hero is MergedFieldWithSameShapeQuery.Data.HumanHero)
    assertEquals(data?.hero?.asHuman()?.property, "Tatooine")
  }

  @Test
  fun canUseExhaustiveWhen() = basicTest(
      "HeroHumanOrDroid.json",
      HeroHumanOrDroidQuery(Optional.Present(Episode.NEWHOPE))
  ) {
    val name = when (val hero = data!!.hero!!) {
      is HeroHumanOrDroidQuery.Data.DroidHero -> hero.name
      is HeroHumanOrDroidQuery.Data.HumanHero -> hero.name
      is HeroHumanOrDroidQuery.Data.OtherHero -> hero.name
    }
    assertEquals(name, "R2-D2")
  }
}
