package com.apollographql.apollo

import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo.cache.normalized.CacheReference
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.Record.Companion.builder
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
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
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
    normalizedCache = apolloClient.getApolloStore().normalizedCache()
  }

  @Test
  @Throws(Exception::class)
  fun testHeroName() {
    assertHasNoErrors("HeroNameResponse.json", HeroNameQuery())
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val reference = record!!.field("hero") as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference("hero"))
    val heroRecord = normalizedCache!!.loadRecord(reference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!.field("name")).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testMergeNull() {
    val record = builder("Key")
        .addField("field1", "value1")
        .build()
    normalizedCache!!.merge(listOf(record), CacheHeaders.NONE)
    val newRecord = record.toBuilder()
        .addField("field2", null)
        .build()
    normalizedCache!!.merge(listOf(newRecord), CacheHeaders.NONE)
    val finalRecord = normalizedCache!!.loadRecord(record.key, CacheHeaders.NONE)
    Truth.assertThat(finalRecord!!.hasField("field2")).isTrue()
    normalizedCache!!.remove(from(record.key))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroNameWithVariable() {
    assertHasNoErrors("EpisodeHeroNameResponse.json", EpisodeHeroNameQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val reference = record!!.field(TEST_FIELD_KEY_JEDI) as CacheReference?
    Truth.assertThat(reference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = normalizedCache!!.loadRecord(reference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!.field("name")).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAppearsInQuery() {
    assertHasNoErrors("HeroAppearsInResponse.json", HeroAppearsInQuery())
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field("hero") as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("hero"))
    val hero = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithoutIDs() {
    assertHasNoErrors("HeroAndFriendsNameResponse.json", HeroAndFriendsNamesQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_JEDI) as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI))
    val heroRecord = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!.field("name")).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.0"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.1"),
        CacheReference(TEST_FIELD_KEY_JEDI + ".friends.2")
    ))
    val luke = normalizedCache!!.loadRecord(TEST_FIELD_KEY_JEDI + ".friends.0", CacheHeaders.NONE)
    Truth.assertThat(luke!!.field("name")).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesQueryWithIDs() {
    assertHasNoErrors("HeroAndFriendsNameWithIdsResponse.json", HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_JEDI) as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("2001"))
    val heroRecord = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!.field("name")).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        CacheReference("1000"),
        CacheReference("1002"),
        CacheReference("1003")
    ))
    val luke = normalizedCache!!.loadRecord("1000", CacheHeaders.NONE)
    Truth.assertThat(luke!!.field("name")).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroAndFriendsNamesWithIDForParentOnly() {
    assertHasNoErrors("HeroAndFriendsNameWithIdsParentOnlyResponse.json",
        HeroAndFriendsNamesWithIDForParentOnlyQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_JEDI) as CacheReference?
    Truth.assertThat(heroReference).isEqualTo(CacheReference("2001"))
    val heroRecord = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(heroRecord!!.field("name")).isEqualTo("R2-D2")
    Truth.assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        CacheReference("2001.friends.0"),
        CacheReference("2001.friends.1"),
        CacheReference("2001.friends.2")
    ))
    val luke = normalizedCache!!.loadRecord("2001.friends.0", CacheHeaders.NONE)
    Truth.assertThat(luke!!.field("name")).isEqualTo("Luke Skywalker")
  }

  @Test
  @Throws(Exception::class)
  fun testSameHeroTwiceQuery() {
    assertHasNoErrors("SameHeroTwiceResponse.json", SameHeroTwiceQuery())
    val record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field("hero") as CacheReference?
    val hero = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!.field("name")).isEqualTo("R2-D2")
    Truth.assertThat(hero.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryDroid() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponse.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.JEDI)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_JEDI) as CacheReference?
    val hero = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!.field("primaryFunction")).isEqualTo("Astromech")
    Truth.assertThat(hero.field("__typename")).isEqualTo("Droid")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroTypeDependentAliasedFieldQueryHuman() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponseHuman.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.EMPIRE)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_EMPIRE) as CacheReference?
    val hero = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!.field("homePlanet")).isEqualTo("Tatooine")
    Truth.assertThat(hero.field("__typename")).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentAliasedFieldQueryHuman() {
    assertHasNoErrors("HeroTypeDependentAliasedFieldResponseHuman.json",
        HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.EMPIRE)))
    val record = normalizedCache!!.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE)
    val heroReference = record!!.field(TEST_FIELD_KEY_EMPIRE) as CacheReference?
    val hero = normalizedCache!!.loadRecord(heroReference!!.key, CacheHeaders.NONE)
    Truth.assertThat(hero!!.field("homePlanet")).isEqualTo("Tatooine")
    Truth.assertThat(hero.field("__typename")).isEqualTo("Human")
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldDroid() {
    assertHasNoErrors("HeroParentTypeDependentFieldDroidResponse.json",
        HeroParentTypeDependentFieldQuery(fromNullable(Episode.JEDI)))
    val lukeRecord = normalizedCache
        .loadRecord(TEST_FIELD_KEY_JEDI + ".friends.0", CacheHeaders.NONE)
    Truth.assertThat(lukeRecord!!.field("name")).isEqualTo("Luke Skywalker")
    Truth.assertThat(lukeRecord.field("height({\"unit\":\"METER\"})")).isEqualTo(BigDecimal.valueOf(1.72))
    val friends = normalizedCache
        .loadRecord(TEST_FIELD_KEY_JEDI, CacheHeaders.NONE)!!.field("friends") as List<Any>?
    Truth.assertThat(friends!![0]).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI + ".friends.0"))
    Truth.assertThat(friends[1]).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI + ".friends.1"))
    Truth.assertThat(friends[2]).isEqualTo(CacheReference(TEST_FIELD_KEY_JEDI + ".friends.2"))
  }

  @Test
  @Throws(Exception::class)
  fun testHeroParentTypeDependentFieldHuman() {
    assertHasNoErrors("HeroParentTypeDependentFieldHumanResponse.json",
        HeroParentTypeDependentFieldQuery(fromNullable(Episode.EMPIRE)))
    val lukeRecord = normalizedCache
        .loadRecord(TEST_FIELD_KEY_EMPIRE + ".friends.0", CacheHeaders.NONE)
    Truth.assertThat(lukeRecord!!.field("name")).isEqualTo("Han Solo")
    Truth.assertThat(lukeRecord.field("height({\"unit\":\"FOOT\"})")).isEqualTo(BigDecimal.valueOf(5.905512))
  }

  @Test
  @Throws(Exception::class)
  fun list_of_objects_with_null_object() {
    assertHasNoErrors("AllPlanetsListOfObjectWithNullObject.json", AllPlanetsQuery())
    val fieldKey = "allPlanets({\"first\":300})"
    var record: Record?

    record = normalizedCache.loadRecord("$fieldKey.planets.0", CacheHeaders.NONE)
    Truth.assertThat(record!!.field("filmConnection")).isNull()
    record = normalizedCache.loadRecord("$fieldKey.planets.0.filmConnection", CacheHeaders.NONE)
    Truth.assertThat(record).isNull()
    record = normalizedCache.loadRecord("$fieldKey.planets.1.filmConnection", CacheHeaders.NONE)
    Truth.assertThat(record).isNotNull()
  }

  @Throws(Exception::class)
  private fun <D: Operation.Data> assertHasNoErrors(mockResponse: String, query: Query<D>) {
    enqueueAndAssertResponse(
        server,
        mockResponse,
        apolloClient!!.query(query)
    ) { response -> !response.hasErrors() }
  }

  companion object {
    private const val TEST_FIELD_KEY_JEDI = "hero({\"episode\":\"JEDI\"})"
    const val TEST_FIELD_KEY_EMPIRE = "hero({\"episode\":\"EMPIRE\"})"
  }
}
