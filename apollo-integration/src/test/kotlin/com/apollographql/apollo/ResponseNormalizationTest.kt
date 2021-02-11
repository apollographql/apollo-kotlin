package com.apollographql.apollo

import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.*
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class ResponseNormalizationTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var normalizedCache: NormalizedCache

  val server = MockWebServer()
  private val QUERY_ROOT_KEY = "QUERY_ROOT"

  @Before
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
    normalizedCache = apolloClient.apolloStore.normalizedCache()
  }

  @Test
  @Throws(Exception::class)
  fun testHeroName() {
    assertHasNoErrors("HeroNameResponse.json", HeroNameQuery())
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val reference = record!!["hero"] as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference("hero"))
    val heroRecord = normalizedCache.loadRecord(reference!!.key, CacheHeaders.NONE)
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
    assertHasNoErrors("EpisodeHeroNameResponse.json", EpisodeHeroNameQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val reference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = normalizedCache.loadRecord(reference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAppearsInQuery() {
    val operation = HeroAppearsInQuery()
    val data = operation.parse(Utils.readFileToString(Utils::class.java, "/HeroAppearsInResponse.json"))
    val records = operation.normalize(data = data.data!!, CustomScalarAdapters.DEFAULT, CacheKeyResolver.DEFAULT)

    val rootRecord = records.first { it.key == QUERY_ROOT_KEY }

    val heroReference = rootRecord["hero"] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("hero"))

    val hero = records.first { it.key == heroReference!!.key }
    Truth.assertThat(hero["appearsIn"]).isEqualTo(listOf("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithoutIDs() {
    assertHasNoErrors("HeroAndFriendsNameResponse.json", HeroAndFriendsNamesQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord["friends"]).isEqualTo(Arrays.asList(
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.0"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.1"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.2")
    ))
    val luke = normalizedCache.loadRecord(TEST_FIELD_KEY_JEDI + ".friends.0", CacheHeaders.NONE)
    Truth.assertThat(luke!!["name"]).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithIDs() {
    assertHasNoErrors("HeroAndFriendsNameWithIdsResponse.json", HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("2001"))
    val heroRecord = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord["friends"]).isEqualTo(Arrays.asList(
        CacheReference("1000"),
        CacheReference("1002"),
        CacheReference("1003")
    ))
    val luke = normalizedCache.loadRecord("1000", CacheHeaders.NONE)
    Truth.assertThat(luke!!["name"]).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesWithIDForParentOnly() {
    val records = records(HeroAndFriendsNamesWithIDForParentOnlyQuery(fromNullable(Episode.JEDI)), "/HeroAndFriendsNameWithIdsParentOnlyResponse.json")
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
    assertHasNoErrors("SameHeroTwiceResponse.json", SameHeroTwiceQuery())
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!["hero"] as CacheReference?
    val hero = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)

    Truth.assertThat(hero!!["name"]).isEqualTo("R2-D2")
    Truth.assertThat(hero["appearsIn"]).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryDroid() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponse.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!![TEST_FIELD_KEY_JEDI] as CacheReference?
    val hero = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!["primaryFunction"]).isEqualTo("Astromech")
    Truth.assertThat(hero["__typename"]).isEqualTo("Droid")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryHuman() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponseHuman.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.EMPIRE)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!["homePlanet"]).isEqualTo("Tatooine")
    Truth.assertThat(hero["__typename"]).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentAliasedFieldQueryHuman() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponseHuman.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.EMPIRE)))
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!![TEST_FIELD_KEY_EMPIRE] as CacheReference?
    val hero = normalizedCache.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!["homePlanet"]).isEqualTo("Tatooine")
    Truth.assertThat(hero["__typename"]).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldDroid() {
    assertHasNoErrors("HeroParentTypeDependentFieldDroidResponse.json",
        HeroParentTypeDependentFieldQuery(fromNullable(Episode.JEDI)))
    val lukeRecord = normalizedCache
        .loadRecord(TEST_FIELD_KEY_JEDI + ".friends.0", CacheHeaders.NONE)
    Truth.assertThat(lukeRecord!!["name"]).isEqualTo("Luke Skywalker")
    Truth.assertThat(lukeRecord["height({\"unit\":\"METER\"})"]).isEqualTo(BigDecimal.valueOf(1.72))
    val friends = normalizedCache
        .loadRecord(TEST_FIELD_KEY_JEDI, CacheHeaders.NONE)!!["friends"] as List<Any>?
    Truth.assertThat(friends!![0]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.0"))
    Truth.assertThat(friends[1]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.1"))
    Truth.assertThat(friends[2]).isEqualTo(CacheReference("$TEST_FIELD_KEY_JEDI.friends.2"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    val records = records(HeroParentTypeDependentFieldQuery(fromNullable(Episode.EMPIRE)), "/HeroParentTypeDependentFieldHumanResponse.json")

    val lukeRecord = records.get("$TEST_FIELD_KEY_EMPIRE.friends.0")
    Truth.assertThat(lukeRecord!!["name"]).isEqualTo("Han Solo")
    Truth.assertThat(lukeRecord["height({\"unit\":\"FOOT\"})"]).isEqualTo(BigDecimal.valueOf(5.905512))
  }

  private fun <D : Operation.Data> records(operation: Operation<D>, name: String): Map<String, Record> {
    val data = operation.parse(Utils.readFileToString(Utils::class.java, name))
    val records = operation.normalize(data = data.data!!, CustomScalarAdapters.DEFAULT, IdFieldCacheKeyResolver())
    return records.associateBy { it.key }
  }

  @Test
  fun list_of_objects_with_null_object2() {
    val records = records(AllPlanetsQuery(), "/AllPlanetsListOfObjectWithNullObject.json")
    val fieldKey = "allPlanets({\"first\":300})"
    var record: Record?

    record = records.get("$fieldKey.planets.0")
    Truth.assertThat(record?.get("filmConnection")).isNull()
    record = records.get("$fieldKey.planets.0.filmConnection")
    Truth.assertThat(record).isNull()
    record = records.get("$fieldKey.planets.1.filmConnection")
    Truth.assertThat(record).isNotNull()
  }

  @Throws(Exception::class)
  private fun <D : Operation.Data> assertHasNoErrors(mockResponse: String, query: Query<D>) {
    enqueueAndAssertResponse(
        server,
        mockResponse,
        apolloClient.query(query)
    ) { response -> !response.hasErrors() }
  }

  companion object {
    private const val TEST_FIELD_KEY_JEDI = "hero({\"episode\":\"JEDI\"})"
    const val TEST_FIELD_KEY_EMPIRE = "hero({\"episode\":\"EMPIRE\"})"
  }
}
