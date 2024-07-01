package test

import IdCacheKeyGenerator
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.api.normalize
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDForParentOnlyQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.HeroAppearsInQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroParentTypeDependentFieldQuery
import com.apollographql.apollo.integration.normalizer.HeroTypeDependentAliasedFieldQuery
import com.apollographql.apollo.integration.normalizer.SameHeroTwiceQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import testFixtureToJsonReader
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for the normalization without an instance of [com.apollographql.apollo.ApolloClient]
 */
class NormalizerTest {
  private lateinit var normalizedCache: NormalizedCache

  private val rootKey = "QUERY_ROOT"

  @BeforeTest
  fun setUp() {
    normalizedCache = MemoryCacheFactory().create()
  }

  @Test
  @Throws(Exception::class)
  fun testHeroName() {
    val records = records(HeroNameQuery(), "HeroNameResponse.json")
    val record = records.get(rootKey)
    val reference = record!!["hero"] as CacheKey?
    assertEquals(reference, CacheKey("hero"))
    val heroRecord = records.get(reference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testMergeNull() {
    val record = Record(
        key = "Key",
        fields = mapOf("field1" to "value1"),
    )
    normalizedCache.merge(listOf(record), CacheHeaders.NONE)

    val newRecord = Record(
        key = "Key",
        fields = mapOf("field2" to null),
    )

    normalizedCache.merge(listOf(newRecord), CacheHeaders.NONE)
    val finalRecord = normalizedCache.loadRecord(record.key, CacheHeaders.NONE)
    assertTrue(finalRecord!!.containsKey("field2"))
    normalizedCache.remove(CacheKey(record.key), false)
  }

  @Test
  @Throws(Exception::class)
  fun testHeroNameWithVariable() {
    val records = records(EpisodeHeroNameQuery(Episode.JEDI), "EpisodeHeroNameResponse.json")
    val record = records.get(rootKey)
    val reference = record!![TEST_FIELD_KEY_JEDI] as CacheKey?
    assertEquals(reference, CacheKey(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(reference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAppearsInQuery() {
    val records = records(HeroAppearsInQuery(), "HeroAppearsInResponse.json")

    val rootRecord = records.get(rootKey)!!

    val heroReference = rootRecord["hero"] as CacheKey?
    assertEquals(heroReference, CacheKey("hero"))

    val hero = records.get(heroReference!!.key)
    assertEquals(hero?.get("appearsIn"), listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithoutIDs() {
    val records = records(HeroAndFriendsNamesQuery(Episode.JEDI), "HeroAndFriendsNameResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheKey?
    assertEquals(heroReference, CacheKey(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheKey("$TEST_FIELD_KEY_JEDI.friends.0"),
            CacheKey("$TEST_FIELD_KEY_JEDI.friends.1"),
            CacheKey("$TEST_FIELD_KEY_JEDI.friends.2")
        ),
        heroRecord["friends"]
    )
    val luke = records.get("$TEST_FIELD_KEY_JEDI.friends.0")
    assertEquals(luke!!["name"], "Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithIDs() {
    val records = records(HeroAndFriendsNamesWithIDsQuery(Episode.JEDI), "HeroAndFriendsNameWithIdsResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheKey?
    assertEquals(heroReference, CacheKey("2001"))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheKey("1000"),
            CacheKey("1002"),
            CacheKey("1003")
        ),
        heroRecord["friends"]
    )
    val luke = records.get("1000")
    assertEquals(luke!!["name"], "Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesWithIDForParentOnly() {
    val records = records(HeroAndFriendsNamesWithIDForParentOnlyQuery(Episode.JEDI), "HeroAndFriendsNameWithIdsParentOnlyResponse.json")
    val record = records[rootKey]
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheKey?
    assertEquals(heroReference, CacheKey("2001"))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheKey("2001.friends.0"),
            CacheKey("2001.friends.1"),
            CacheKey("2001.friends.2")
        ),
        heroRecord["friends"]
    )
    val luke = records.get("2001.friends.0")
    assertEquals(luke!!["name"], "Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testSameHeroTwiceQuery() {
    val records = records(SameHeroTwiceQuery(), "SameHeroTwiceResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!!["hero"] as CacheKey?
    val hero = records.get(heroReference!!.key)

    assertEquals(hero!!["name"], "R2-D2")
    assertEquals(hero["appearsIn"], listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryDroid() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Episode.JEDI), "HeroTypeDependentAliasedFieldResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheKey?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["primaryFunction"], "Astromech")
    assertEquals(hero["__typename"], "Droid")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Episode.EMPIRE), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheKey?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["homePlanet"], "Tatooine")
    assertEquals(hero["__typename"], "Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Episode.EMPIRE), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheKey?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["homePlanet"], "Tatooine")
    assertEquals(hero["__typename"], "Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldDroid() {
    val records = records(HeroParentTypeDependentFieldQuery(Episode.JEDI), "HeroParentTypeDependentFieldDroidResponse.json")
    val lukeRecord = records.get(TEST_FIELD_KEY_JEDI + ".friends.0")
    assertEquals(lukeRecord!!["name"], "Luke Skywalker")
    assertEquals(lukeRecord["height({\"unit\":\"METER\"})"], 1.72)


    val friends = records[TEST_FIELD_KEY_JEDI]!!["friends"]

    assertIs<List<Any>>(friends)
    assertEquals(friends[0], CacheKey("$TEST_FIELD_KEY_JEDI.friends.0"))
    assertEquals(friends[1], CacheKey("$TEST_FIELD_KEY_JEDI.friends.1"))
    assertEquals(friends[2], CacheKey("$TEST_FIELD_KEY_JEDI.friends.2"))
  }

  @Test
  fun list_of_objects_with_null_object() {
    val records = records(AllPlanetsQuery(), "AllPlanetsListOfObjectWithNullObject.json")
    val fieldKey = "allPlanets({\"first\":300})"

    var record: Record? = records["$fieldKey.planets.0"]
    assertTrue(record?.get("filmConnection") == null)
    record = records.get("$fieldKey.planets.0.filmConnection")
    assertTrue(record == null)
    record = records.get("$fieldKey.planets.1.filmConnection")
    assertTrue(record != null)
  }


  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    val records = records(HeroParentTypeDependentFieldQuery(Episode.EMPIRE), "HeroParentTypeDependentFieldHumanResponse.json")

    val lukeRecord = records.get("$TEST_FIELD_KEY_EMPIRE.friends.0")
    assertEquals(lukeRecord!!["name"], "Han Solo")
    assertEquals(lukeRecord["height({\"unit\":\"FOOT\"})"], 5.905512)
  }

  companion object {
    internal fun <D : Operation.Data> records(operation: Operation<D>, name: String): Map<String, Record> {
      val response = testFixtureToJsonReader(name).toApolloResponse(operation)
      return operation.normalize(data = response.data!!, CustomScalarAdapters.Empty, cacheKeyGenerator = IdCacheKeyGenerator)
    }

    private const val TEST_FIELD_KEY_JEDI = "hero({\"episode\":\"JEDI\"})"
    const val TEST_FIELD_KEY_EMPIRE = "hero({\"episode\":\"EMPIRE\"})"
  }
}
