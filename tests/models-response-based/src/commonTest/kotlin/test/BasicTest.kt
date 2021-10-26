package test

import IdObjectIdGenerator
import codegen.models.HeroHumanOrDroidQuery
import codegen.models.HeroParentTypeDependentFieldQuery
import codegen.models.HeroParentTypeDependentFieldQuery.Data.DroidHero.Friend.Companion.asHuman
import codegen.models.HeroParentTypeDependentFieldQuery.Data.Hero.Companion.asDroid
import codegen.models.MergedFieldWithSameShapeQuery
import codegen.models.MergedFieldWithSameShapeQuery.Data.Hero.Companion.asDroid
import codegen.models.MergedFieldWithSameShapeQuery.Data.Hero.Companion.asHuman
import codegen.models.type.Episode
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import readJson
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
        objectIdGenerator = IdObjectIdGenerator
    )
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  private fun <D : Query.Data> basicTest(resourceName: String, query: Query<D>, block: ApolloResponse<D>.() -> Unit) = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(readJson(resourceName))
    var response = apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.NetworkOnly).build())
    response.block()
    response = apolloClient.query(ApolloRequest.Builder(query).fetchPolicy(FetchPolicy.CacheOnly).build())
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
    val hero = data?.hero?.asDroid()!!
    assertEquals(hero.friends?.size, 3)
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals(hero.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals((hero.friends?.get(0)?.asHuman())?.height, 1.72)
  }


  @Test
  fun polymorphicDroidFieldsGetParsedToDroid() = basicTest(
      "MergedFieldWithSameShape_Droid.json",
      MergedFieldWithSameShapeQuery(Episode.NEWHOPE)
  ) {

    assertFalse(hasErrors())
    assertTrue(data?.hero is MergedFieldWithSameShapeQuery.Data.DroidHero)
    assertEquals(data?.hero?.asDroid()?.property, "Astromech")
  }

  @Test
  fun polymorphicHumanFieldsGetParsedToHuman() = basicTest(
      "MergedFieldWithSameShape_Human.json",
      MergedFieldWithSameShapeQuery(Episode.NEWHOPE)
  ) {

    assertFalse(hasErrors())
    assertTrue(data?.hero is MergedFieldWithSameShapeQuery.Data.HumanHero)
    assertEquals(data?.hero?.asHuman()?.property, "Tatooine")
  }

  @Test
  fun canUseExhaustiveWhen() = basicTest(
      "HeroHumanOrDroid.json",
      HeroHumanOrDroidQuery(Episode.NEWHOPE)
  ) {
    val name = when (val hero = data!!.hero!!) {
      is HeroHumanOrDroidQuery.Data.DroidHero -> hero.name
      is HeroHumanOrDroidQuery.Data.HumanHero -> hero.name
      is HeroHumanOrDroidQuery.Data.OtherHero -> hero.name
    }
    assertEquals(name, "R2-D2")
  }
}
