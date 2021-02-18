package com.apollographql.apollo3

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.parse
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo3.cache.normalized.CacheReference
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.normalize
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
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import java.util.Arrays

class ResponseNormalizationTest {
  private lateinit var normalizedCache: NormalizedCache

  private val QUERY_ROOT_KEY = "QUERY_ROOT"

  @Before
  fun setUp() {
    normalizedCache = MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE).create()
  }

  @Test
  @Throws(Exception::class)
  fun testHeroName() {
    val records = records(HeroNameQuery(), "HeroNameResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val reference = record!!["hero"] as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference("hero"))
    val heroRecord = records.get(reference!!.key)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
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
    Truth.assertThat(finalRecord!!.containsKey("field2")).isTrue()
    normalizedCache.remove(from(record.key), false)
  }

  @Test
  @Throws(Exception::class)
  fun testHeroNameWithVariable() {
    val records = records(EpisodeHeroNameQuery(Input.present(Episode.JEDI)), "EpisodeHeroNameResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val reference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(reference!!.key)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAppearsInQuery() {
    val records = records(HeroAppearsInQuery(), "HeroAppearsInResponse.json")

    val rootRecord = records.get(QUERY_ROOT_KEY)!!

    val heroReference = rootRecord["hero"] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("hero"))

    val hero = records.get(heroReference!!.key)
    Truth.assertThat(hero?.get("appearsIn")).isEqualTo(listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithoutIDs() {
    val records = records(HeroAndFriendsNamesQuery(Input.present(Episode.JEDI)), "HeroAndFriendsNameResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = records.get(heroReference!!.key)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord["friends"]).isEqualTo(Arrays.asList(
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.0"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.1"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.2")
    ))
    val luke = records.get(TEST_FIELD_KEY_JEDI + ".friends.0")
    Truth.assertThat(luke!!["name"]).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithIDs() {
    val records = records(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.JEDI)), "HeroAndFriendsNameWithIdsResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("2001"))
    val heroRecord = records.get(heroReference!!.key)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord["friends"]).isEqualTo(Arrays.asList(
        CacheReference("1000"),
        CacheReference("1002"),
        CacheReference("1003")
    ))
    val luke = records.get("1000")
    Truth.assertThat(luke!!["name"]).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesWithIDForParentOnly() {
    val records = records(HeroAndFriendsNamesWithIDForParentOnlyQuery(Input.present(Episode.JEDI)), "HeroAndFriendsNameWithIdsParentOnlyResponse.json")
    val record = records[QUERY_ROOT_KEY]
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("2001"))
    val heroRecord = records.get(heroReference!!.key)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord["friends"]).isEqualTo(Arrays.asList(
        CacheReference("2001.friends.0"),
        CacheReference("2001.friends.1"),
        CacheReference("2001.friends.2")
    ))
    val luke = records.get("2001.friends.0")
    Truth.assertThat(luke!!["name"]).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testSameHeroTwiceQuery() {
    val records = records(SameHeroTwiceQuery(), "SameHeroTwiceResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!!["hero"] as CacheReference?
    val hero = records.get(heroReference!!.key)

    Truth.assertThat(hero!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(hero["appearsIn"]).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryDroid() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Input.present(Episode.JEDI)), "HeroTypeDependentAliasedFieldResponse.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    val hero = records.get(heroReference!!.key)
    Truth.assertThat(hero!!["primaryFunction"]).isEqualTo("Astromech")
    Truth.assertThat(hero["__typename"]).isEqualTo("Droid")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Input.present(Episode.EMPIRE)), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = records.get(heroReference!!.key)
    Truth.assertThat(hero!!["homePlanet"]).isEqualTo("Tatooine")
    Truth.assertThat(hero["__typename"]).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentAliasedFieldQueryHuman() {
    val records = records(HeroTypeDependentAliasedFieldQuery(Input.present(Episode.EMPIRE)), "HeroTypeDependentAliasedFieldResponseHuman.json")
    val record = records.get(QUERY_ROOT_KEY)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = records.get(heroReference!!.key)
    Truth.assertThat(hero!!["homePlanet"]).isEqualTo("Tatooine")
    Truth.assertThat(hero["__typename"]).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldDroid() {
    val records = records(HeroParentTypeDependentFieldQuery(Input.present(Episode.JEDI)), "HeroParentTypeDependentFieldDroidResponse.json")
    val lukeRecord = records.get(TEST_FIELD_KEY_JEDI + ".friends.0")
    Truth.assertThat(lukeRecord!!["name"]).isEqualTo("Luke Skywalker")
    Truth.assertThat(lukeRecord["height({\"unit\":\"METER\"})"]).isEqualTo(1.72)
    val friends = records.get(TEST_FIELD_KEY_JEDI)!!["friends"] as List<Any>?
    Truth.assertThat(friends!![0]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.0"))
    Truth.assertThat(friends[1]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.1"))
    Truth.assertThat(friends[2]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.2"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    val records = records(HeroParentTypeDependentFieldQuery(Input.present(Episode.EMPIRE)), "HeroParentTypeDependentFieldHumanResponse.json")

    val lukeRecord = records.get("$TEST_FIELD_KEY_EMPIRE.friends.0")
    Truth.assertThat(lukeRecord!!["name"]).isEqualTo("Han Solo")
    Truth.assertThat(lukeRecord["height({\"unit\":\"FOOT\"})"]).isEqualTo(5.905512)
  }

  @Test
  fun list_of_objects_with_null_object() {
    val records = records(AllPlanetsQuery(), "AllPlanetsListOfObjectWithNullObject.json")
    val fieldKey = "allPlanets({\"first\":300})"
    var record: Record?

    record = records.get("$fieldKey.planets.0")
    Truth.assertThat(record?.get("filmConnection")).isNull()
    record = records.get("$fieldKey.planets.0.filmConnection")
    Truth.assertThat(record).isNull()
    record = records.get("$fieldKey.planets.1.filmConnection")
    Truth.assertThat(record).isNotNull()
  }

  companion object {
    private fun <D : Operation.Data> records(operation: Operation<D>, name: String): Map<String, Record> {
      val data = operation.parse(Utils.readResource(name))
      return operation.normalize(data = data.data!!, ResponseAdapterCache.DEFAULT, IdFieldCacheKeyResolver())
    }

    private const val TEST_FIELD_KEY_JEDI = "hero({\"episode\":\"JEDI\"})"
    const val TEST_FIELD_KEY_EMPIRE = "hero({\"episode\":\"EMPIRE\"})"
  }
}
