package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.assertResponse
import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroWithDatesQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroWithInlineFragmentQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsWithFragmentsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameWithEnumsQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery.Data.Starship
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragment
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragmentImpl
import com.apollographql.apollo3.integration.normalizer.fragment.HumanWithIdFragment
import com.apollographql.apollo3.integration.normalizer.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo3.integration.normalizer.type.CustomScalars
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth.assertThat
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResponseWriteTestCase {
  private var apolloClient: ApolloClient? = null

  private val server = MockWebServer()
  private val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  private val dateCustomScalarAdapter: ResponseAdapter<Date> = object : ResponseAdapter<Date> {

    override fun fromResponse(reader: JsonReader): Date {
      return DATE_TIME_FORMAT.parse(reader.nextString())
    }

    override fun toResponse(writer: JsonWriter, value: Date) {
      writer.value(DATE_TIME_FORMAT.format(value))
    }
  }

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
        .addCustomScalarAdapter(CustomScalars.Date, dateCustomScalarAdapter)
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun customScalar() {
    val query = EpisodeHeroWithDatesQuery(Input.Present(Episode.JEDI))
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroWithDatesResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.heroName).isEqualTo("R2-D2")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.birthDate)).isEqualTo("1984-04-16")
      assertThat(it.data?.hero?.showUpDates).hasSize(3)
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.showUpDates?.get(0))).isEqualTo("2017-01-16")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.showUpDates?.get(1))).isEqualTo("2017-02-16")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.showUpDates?.get(2))).isEqualTo("2017-03-16")
    }
    var hero = EpisodeHeroWithDatesQuery.Data.Hero(
        "R222-D222",
        DATE_TIME_FORMAT.parse("1985-04-16"), emptyList())
    apolloClient!!.apolloStore.writeOperation(query, EpisodeHeroWithDatesQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.heroName).isEqualTo("R222-D222")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.birthDate)).isEqualTo("1985-04-16")
      assertThat(it.data?.hero?.showUpDates).hasSize(0)
    }
    hero = EpisodeHeroWithDatesQuery.Data.Hero(
        "R22-D22",
        DATE_TIME_FORMAT.parse("1986-04-16"),
        listOf(
            DATE_TIME_FORMAT.parse("2017-04-16"),
            DATE_TIME_FORMAT.parse("2017-05-16")
        )
    )
    apolloClient!!.apolloStore.writeOperation(query, EpisodeHeroWithDatesQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.heroName).isEqualTo("R22-D22")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.birthDate)).isEqualTo("1986-04-16")
      assertThat(it.data?.hero?.showUpDates).hasSize(2)
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.showUpDates?.get(0))).isEqualTo("2017-04-16")
      assertThat(DATE_TIME_FORMAT.format(it.data?.hero?.showUpDates?.get(1))).isEqualTo("2017-05-16")
    }
  }

  @Test
  @Throws(Exception::class)
  fun enums() {
    val query = HeroNameWithEnumsQuery()
    enqueueAndAssertResponse(
        server,
        "HeroNameWithEnumsResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R2-D2")
      assertThat(it.data?.hero?.firstAppearsIn).isEqualTo(Episode.EMPIRE)
      assertThat(it.data?.hero?.appearsIn).hasSize(3)
      assertThat(it.data?.hero?.appearsIn?.get(0)).isEqualTo(Episode.NEWHOPE)
      assertThat(it.data?.hero?.appearsIn?.get(1)).isEqualTo(Episode.EMPIRE)
      assertThat(it.data?.hero?.appearsIn?.get(2)).isEqualTo(Episode.JEDI)
    }
    var hero = HeroNameWithEnumsQuery.Data.Hero(
        "R222-D222",
        Episode.JEDI, emptyList<Episode>())
    apolloClient!!.apolloStore.writeOperation(query, HeroNameWithEnumsQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R222-D222")
      assertThat(it.data?.hero?.firstAppearsIn).isEqualTo(Episode.JEDI)
      assertThat(it.data?.hero?.appearsIn).hasSize(0)
    }
    hero = HeroNameWithEnumsQuery.Data.Hero(
        "R22-D22",
        Episode.JEDI,
        listOf(Episode.EMPIRE)
    )
    apolloClient!!.apolloStore.writeOperation(query, HeroNameWithEnumsQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R22-D22")
      assertThat(it.data?.hero?.firstAppearsIn).isEqualTo(Episode.JEDI)
      assertThat(it.data?.hero?.appearsIn).hasSize(1)
      assertThat(it.data?.hero?.appearsIn?.get(0)).isEqualTo(Episode.EMPIRE)
    }
  }

  @Test
  @Throws(Exception::class)
  fun objects() {
    val query = HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.JEDI))
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R2-D2")
      assertThat(it.data?.hero?.id).isEqualTo("2001")
      assertThat(it.data?.hero?.friends).hasSize(3)
      assertThat(it.data?.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(it.data?.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(it.data?.hero?.friends?.get(1)?.id).isEqualTo("1002")
      assertThat(it.data?.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(it.data?.hero?.friends?.get(2)?.id).isEqualTo("1003")
      assertThat(it.data?.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
    }
    var hero = HeroAndFriendsNamesWithIDsQuery.Data.Hero(
        "2001",
        "R222-D222",
        null
    )
    apolloClient!!.apolloStore.writeOperation(query, HeroAndFriendsNamesWithIDsQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R222-D222")
      assertThat(it.data?.hero?.id).isEqualTo("2001")
      assertThat(it.data?.hero?.friends).isNull()
    }
    val friend = HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friends(
        "1002",
        "Han Soloooo"
    )
    hero = HeroAndFriendsNamesWithIDsQuery.Data.Hero(
        hero.id,
        "R222-D222",
        listOf(friend)
    )
    apolloClient!!.apolloStore.writeOperation(query, HeroAndFriendsNamesWithIDsQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R222-D222")
      assertThat(it.data?.hero?.id).isEqualTo("2001")
      assertThat(it.data?.hero?.friends).hasSize(1)
      assertThat(it.data?.hero?.friends?.get(0)?.id).isEqualTo("1002")
      assertThat(it.data?.hero?.friends?.get(0)?.name).isEqualTo("Han Soloooo")
    }
  }

  @Test
  @Throws(Exception::class)
  fun operation_with_fragments() {
    val query = HeroAndFriendsWithFragmentsQuery(Input.Present(Episode.NEWHOPE))
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsWithFragmentResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.id).isEqualTo("2001")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.name).isEqualTo("R2-D2")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.friends).hasSize(3)
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.id).isEqualTo("1000")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.name).isEqualTo("Luke Skywalker")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.id).isEqualTo("1002")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.name).isEqualTo("Han Solo")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(2) as? HumanWithIdFragment)?.__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(2) as? HumanWithIdFragment)?.id).isEqualTo("1003")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(2) as? HumanWithIdFragment)?.name).isEqualTo("Leia Organa")
    }
    val hero = HeroAndFriendsWithFragmentsQuery.Data.Hero.CharacterHero(
        __typename = "Droid",
        id = "2001",
        name = "R222-D222",
        friends = listOf(
            HeroAndFriendsWithFragmentsQuery.Data.Hero.CharacterHero.Friends.HumanFriends(
                __typename = "Human",
                id = "1006",
                name = "SuperMan"
            ),
            HeroAndFriendsWithFragmentsQuery.Data.Hero.CharacterHero.Friends.HumanFriends(
                __typename = "Human",
                id = "1004",
                name = "Beast"
            )
        )
    )
    apolloClient!!.apolloStore.writeOperation(query, HeroAndFriendsWithFragmentsQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.id).isEqualTo("2001")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.name).isEqualTo("R222-D222")
      assertThat((it.data?.hero as? HeroWithFriendsFragment)?.friends).hasSize(2)
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.id).isEqualTo("1006")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(0) as? HumanWithIdFragment)?.name).isEqualTo("SuperMan")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.id).isEqualTo("1004")
      assertThat(((it.data?.hero as? HeroWithFriendsFragment)?.friends?.get(1) as? HumanWithIdFragment)?.name).isEqualTo("Beast")
    }
  }

  @Test
  @Throws(Exception::class)
  fun operation_with_inline_fragments() {
    val query = EpisodeHeroWithInlineFragmentQuery(Input.Present(Episode.NEWHOPE))
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroWithInlineFragmentResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R2-D2")
      assertThat(it.data?.hero?.friends).hasSize(3)
      val asHuman = it.data?.hero?.friends?.get(0) as? EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.HumanFriends
      assertThat(asHuman?.__typename).isEqualTo("Human")
      assertThat(asHuman?.id).isEqualTo("1000")
      assertThat(asHuman?.name).isEqualTo("Luke Skywalker")
      assertThat(asHuman?.height).isWithin(1.5)
      val asDroid1 = it.data?.hero?.friends?.get(1) as? EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.DroidFriends
      assertThat(asDroid1?.__typename).isEqualTo("Droid")
      assertThat(asDroid1?.name).isEqualTo("Android")
      assertThat(asDroid1?.primaryFunction).isEqualTo("Hunt and destroy iOS devices")
      val asDroid2 = it.data?.hero?.friends?.get(2) as EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.DroidFriends
      assertThat(asDroid2.__typename).isEqualTo("Droid")
      assertThat(asDroid2.name).isEqualTo("Battle Droid")
      assertThat(asDroid2.primaryFunction).isEqualTo("Controlled alternative to human soldiers")
    }
    val hero = EpisodeHeroWithInlineFragmentQuery.Data.Hero(
        name = "R22-D22",
        friends = listOf(
            EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.HumanFriends(
                __typename = "Human",
                id = "1002",
                name = "Han Solo",
                height = 2.5
            ),
            EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.DroidFriends(
                __typename = "Droid",
                primaryFunction = "Entertainment",
                name = "RD",
            ),
        )
    )
    apolloClient!!.apolloStore.writeOperation(query, EpisodeHeroWithInlineFragmentQuery.Data(hero))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.name).isEqualTo("R22-D22")
      assertThat(it.data?.hero?.friends).hasSize(2)
      val asHuman = it.data?.hero?.friends?.get(0) as? EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.HumanFriends
      assertThat(asHuman?.__typename).isEqualTo("Human")
      assertThat(asHuman?.id).isEqualTo("1002")
      assertThat(asHuman?.name).isEqualTo("Han Solo")
      assertThat(asHuman?.height).isWithin(2.5)
      val asDroid = it.data?.hero?.friends?.get(1) as? EpisodeHeroWithInlineFragmentQuery.Data.Hero.Friends.DroidFriends
      assertThat(asDroid?.__typename).isEqualTo("Droid")
      assertThat(asDroid?.name).isEqualTo("RD")
      assertThat(asDroid?.primaryFunction).isEqualTo("Entertainment")
    }
  }

  @Test
  @Throws(Exception::class)
  fun fragments() {
    val query = HeroAndFriendsWithFragmentsQuery(Input.Present(Episode.NEWHOPE))
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsWithFragmentResponse.json",
        apolloClient!!.query(query)
    ) {
      assertThat(it.data?.hero?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as HeroWithFriendsFragment).__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as HeroWithFriendsFragment).id).isEqualTo("2001")
      assertThat((it.data?.hero as HeroWithFriendsFragment).name).isEqualTo("R2-D2")
      assertThat((it.data?.hero as HeroWithFriendsFragment).friends).hasSize(3)
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).id).isEqualTo("1000")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).name).isEqualTo("Luke Skywalker")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).id).isEqualTo("1002")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).name).isEqualTo("Han Solo")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(2) as HumanWithIdFragment).__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(2) as HumanWithIdFragment).id).isEqualTo("1003")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(2) as HumanWithIdFragment).name).isEqualTo("Leia Organa")
    }

    apolloClient!!.apolloStore.writeFragment(
        HeroWithFriendsFragmentImpl(),
        from("2001"),
        HeroWithFriendsFragmentImpl.Data(
            __typename = "Droid",
            id = "2001",
            name = "R222-D222",
            friends = listOf(
                HeroWithFriendsFragmentImpl.Data.Friends.HumanFriends(
                    __typename = "Human",
                    id = "1000",
                    name = "SuperMan"
                ),
                HeroWithFriendsFragmentImpl.Data.Friends.HumanFriends(
                    __typename = "Human",
                    id = "1002",
                    name = "Han Solo"
                ),
            )
        )
    )
    apolloClient!!.apolloStore.writeFragment(
        HumanWithIdFragmentImpl(),
        from("1002"),
        HumanWithIdFragmentImpl.Data(
            __typename = "Human",
            id = "1002",
            name = "Beast"
        )
    )
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.hero?.__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as HeroWithFriendsFragment).__typename).isEqualTo("Droid")
      assertThat((it.data?.hero as HeroWithFriendsFragment).id).isEqualTo("2001")
      assertThat((it.data?.hero as HeroWithFriendsFragment).name).isEqualTo("R222-D222")
      assertThat((it.data?.hero as HeroWithFriendsFragment).friends).hasSize(2)
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).id).isEqualTo("1000")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(0) as HumanWithIdFragment).name).isEqualTo("SuperMan")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).__typename).isEqualTo("Human")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).id).isEqualTo("1002")
      assertThat(((it.data?.hero as HeroWithFriendsFragment).friends?.get(1) as HumanWithIdFragment).name).isEqualTo("Beast")
    }
  }

  @Test
  @Throws(Exception::class)
  fun listOfList() {
    val query = StarshipByIdQuery("Starship1")
    enqueueAndAssertResponse(
        server,
        "StarshipByIdResponse.json",
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) {
      assertThat(it.data?.starship?.name).isEqualTo("SuperRocket")
      assertThat(it.data?.starship?.coordinates).hasSize(3)
      assertThat(it.data?.starship?.coordinates).containsExactly(
          listOf(100.0, 200.0),
          listOf(300.0, 400.0),
          listOf(500.0, 600.0)
      )
    }
    val starship = Starship(
        "Starship1",
        "SuperRocket",
        listOf(
            listOf(900.0, 800.0),
            listOf(700.0, 600.0)
        )
    )
    apolloClient!!.apolloStore.writeOperation(query, StarshipByIdQuery.Data(starship))
    assertCachedQueryResponse(
        query
    ) {
      assertThat(it.data?.starship?.name).isEqualTo("SuperRocket")
      assertThat(it.data?.starship?.coordinates).hasSize(2)
      assertThat(it.data?.starship?.coordinates).containsExactly(
          listOf(900.0, 800.0),
          listOf(700.0, 600.0)
      )
    }
  }

  @Throws(Exception::class)
  private fun <D: Operation.Data> assertCachedQueryResponse(query: Query<D>, block: (ApolloResponse<D>) -> Unit) {
    assertResponse(
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY), block
    )
  }
}
