package com.apollographql.apollo3.integration.test.noclient

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.fromResponse
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.normalize
import com.apollographql.apollo3.integration.IdFieldCacheKeyResolver
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDForParentOnlyQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAppearsInQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroParentTypeDependentFieldQuery
import com.apollographql.apollo3.integration.normalizer.HeroTypeDependentAliasedFieldQuery
import com.apollographql.apollo3.integration.normalizer.SameHeroTwiceQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import com.apollographql.apollo3.integration.assertEquals2 as assertEquals

/**
 * Tests for the normalization without an instance of [ApolloClient]
 */
class NormalizerTest {
  private lateinit var normalizedCache: NormalizedCache

  private val rootKey = "QUERY_ROOT"

  @BeforeTest
  fun setUp() {
    normalizedCache = MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE).create()
  }


  @Test
  @Throws(Exception::class)
  fun testHeroName() {
    val records = records(HeroNameQuery(), "HeroNameResponse.json")
    val record = records.get(rootKey)
    val reference = record!!["hero"] as CacheReference?
    assertEquals(reference, CacheReference("hero"))
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
    normalizedCache.remove(from(record.key), false)
  }

  @Test
  @Throws(Exception::class)
  fun testHeroNameWithVariable() {
    val records = records(EpisodeHeroNameQuery(Optional.Present(Episode.JEDI)), "EpisodeHeroNameResponse.json")
    val record = records.get(rootKey)
    val reference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    assertEquals(reference, CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(reference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAppearsInQuery() {
    val records = records(HeroAppearsInQuery(), "HeroAppearsInResponse.json")

    val rootRecord = records.get(rootKey)!!

    val heroReference = rootRecord["hero"] as CacheReference?
    assertEquals(heroReference, CacheReference("hero"))

    val hero = records.get(heroReference!!.key)
    assertEquals(hero?.get("appearsIn"), listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithoutIDs() {
    val records = records(HeroAndFriendsNamesQuery(Optional.Present(Episode.JEDI)), "HeroAndFriendsNameResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    assertEquals(heroReference, CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheReference("$TEST_FIELD_KEY_JEDI.friends.0"),
            CacheReference("$TEST_FIELD_KEY_JEDI.friends.1"),
            CacheReference("$TEST_FIELD_KEY_JEDI.friends.2")
        ),
        heroRecord["friends"]
    )
    val luke = records.get("$TEST_FIELD_KEY_JEDI.friends.0")
    assertEquals(luke!!["name"], "Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithIDs() {
    val records = records(HeroAndFriendsNamesWithIDsQuery(Optional.Present(Episode.JEDI)), "HeroAndFriendsNameWithIdsResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    assertEquals(heroReference, CacheReference("2001"))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheReference("1000"),
            CacheReference("1002"),
            CacheReference("1003")
        ),
        heroRecord["friends"]
    )
    val luke = records.get("1000")
    assertEquals(luke!!["name"], "Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesWithIDForParentOnly() {
    val records = records(HeroAndFriendsNamesWithIDForParentOnlyQuery(Optional.Present(Episode.JEDI)), "HeroAndFriendsNameWithIdsParentOnlyResponse.json")
    val record = records[rootKey]
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    assertEquals(heroReference, CacheReference("2001"))
    val heroRecord = records.get(heroReference!!.key)
    assertEquals(heroRecord!!["name"], "R2-D2")
    assertEquals(
        listOf(
            CacheReference("2001.friends.0"),
            CacheReference("2001.friends.1"),
            CacheReference("2001.friends.2")
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
    val heroReference = record!!["hero"] as CacheReference?
    val hero = records.get(heroReference!!.key)

    assertEquals(hero!!["name"], "R2-D2")
    assertEquals(hero["appearsIn"], listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryDroid() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Optional.Present(Episode.JEDI)), "HeroTypeDependentAliasedFieldResponse.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["primaryFunction"], "Astromech")
    assertEquals(hero["__typename"], "Droid")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Optional.Present(Episode.EMPIRE)), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["homePlanet"], "Tatooine")
    assertEquals(hero["__typename"], "Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Optional.Present(Episode.EMPIRE)), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(rootKey)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = records.get(heroReference!!.key)
    assertEquals(hero!!["homePlanet"], "Tatooine")
    assertEquals(hero["__typename"], "Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldDroid() {
    val records = records(HeroParentTypeDependentFieldQuery(Optional.Present(Episode.JEDI)), "HeroParentTypeDependentFieldDroidResponse.json")
    val lukeRecord = records.get(TEST_FIELD_KEY_JEDI + ".friends.0")
    assertEquals(lukeRecord!!["name"], "Luke Skywalker")
    assertEquals(lukeRecord["height({\"unit\":\"METER\"})"], 1.72)
    val friends = records.get(TEST_FIELD_KEY_JEDI)!!["friends"] as List<Any>?
    assertEquals(friends!![0], CacheReference("$TEST_FIELD_KEY_JEDI.friends.0"))
    assertEquals(friends[1], CacheReference("$TEST_FIELD_KEY_JEDI.friends.1"))
    assertEquals(friends[2], CacheReference("$TEST_FIELD_KEY_JEDI.friends.2"))
  }

  @Test
  fun list_of_objects_with_null_object() {
    val records = records(AllPlanetsQuery(), "AllPlanetsListOfObjectWithNullObject.json")
    val fieldKey = "allPlanets({\"first\":300})"
    var record: Record?

    record = records.get("$fieldKey.planets.0")
    assertTrue(record?.get("filmConnection") == null)
    record = records.get("$fieldKey.planets.0.filmConnection")
    assertTrue(record == null)
    record = records.get("$fieldKey.planets.1.filmConnection")
    assertTrue(record != null)
  }


  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    val records = records(HeroParentTypeDependentFieldQuery(Optional.Present(Episode.EMPIRE)), "HeroParentTypeDependentFieldHumanResponse.json")

    val lukeRecord = records.get("$TEST_FIELD_KEY_EMPIRE.friends.0")
    assertEquals(lukeRecord!!["name"], "Han Solo")
    assertEquals(lukeRecord["height({\"unit\":\"FOOT\"})"], 5.905512)
  }

  companion object {
    internal fun <D : Operation.Data> records(operation: Operation<D>, name: String): Map<String, Record> {
      val response = operation.fromResponse(readResource(name))
      return operation.normalize(data = response.data!!, ResponseAdapterCache.DEFAULT, IdFieldCacheKeyResolver)
    }

    private const val TEST_FIELD_KEY_JEDI = "hero({\"episode\":\"JEDI\"})"
    const val TEST_FIELD_KEY_EMPIRE = "hero({\"episode\":\"EMPIRE\"})"
  }
}
