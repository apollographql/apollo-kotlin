package com.apollographql.apollo

import com.apollographql.apollo.Utils.assertResponse
import com.apollographql.apollo.Utils.cacheAndAssertCachedResponse
import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.parse
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCache.Companion.prettifyDump
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.benchmark.GetResponseQuery
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo.integration.normalizer.*
import com.apollographql.apollo.integration.normalizer.fragment.HeroWithFriendsFragmentImpl
import com.apollographql.apollo.integration.normalizer.fragment.HumanWithIdFragment
import com.apollographql.apollo.integration.normalizer.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import java.util.*
import java.util.concurrent.TimeUnit

class NormalizedCacheTestCase {
  private lateinit var apolloClient: ApolloClient

  val server = MockWebServer()

  @Before
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun episodeHeroName() {
    cacheAndAssertCachedResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNameResponse() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(HeroAndFriendsNamesQuery(fromNullable(Episode.JEDI)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.friends).hasSize(3)
      assertThat(response.data!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(response.data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(response.data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNamesWithIDs() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.id).isEqualTo("2001")
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.friends).hasSize(3)
      assertThat(response.data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(response.data!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(response.data!!.hero?.friends?.get(1)?.id).isEqualTo("1002")
      assertThat(response.data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(response.data!!.hero?.friends?.get(2)?.id).isEqualTo("1003")
      assertThat(response.data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNameWithIdsForParentOnly() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsParentOnlyResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDForParentOnlyQuery(fromNullable(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.id).isEqualTo("2001")
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.friends).hasSize(3)
      assertThat(response.data!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(response.data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(response.data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAppearsInResponse() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAppearsInResponse.json",
        apolloClient.query(HeroAppearsInQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.appearsIn).hasSize(3)
      assertThat(response.data!!.hero?.appearsIn?.get(0)?.name).isEqualTo("NEWHOPE")
      assertThat(response.data!!.hero?.appearsIn?.get(1)?.name).isEqualTo("EMPIRE")
      assertThat(response.data!!.hero?.appearsIn?.get(2)?.name).isEqualTo("JEDI")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAppearsInResponseWithNulls() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAppearsInResponseWithNulls.json",
        apolloClient.query(HeroAppearsInQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.appearsIn).hasSize(6)
      assertThat(response.data!!.hero?.appearsIn?.get(0)).isNull()
      assertThat(response.data!!.hero?.appearsIn?.get(1)?.name).isEqualTo("NEWHOPE")
      assertThat(response.data!!.hero?.appearsIn?.get(2)?.name).isEqualTo("EMPIRE")
      assertThat(response.data!!.hero?.appearsIn?.get(3)).isNull()
      assertThat(response.data!!.hero?.appearsIn?.get(4)?.name).isEqualTo("JEDI")
      assertThat(response.data!!.hero?.appearsIn?.get(5)).isNull()
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroParentTypeDependentField() {
    cacheAndAssertCachedResponse(
        server,
        "HeroParentTypeDependentFieldDroidResponse.json",
        apolloClient.query(HeroParentTypeDependentFieldQuery(fromNullable(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      val hero = response.data!!.hero as HeroParentTypeDependentFieldQuery.Data.Hero.Droid
      assertThat(hero.friends).hasSize(3)
      assertThat(hero.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(hero.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat((hero.friends?.get(0) as HeroParentTypeDependentFieldQuery.Data.Hero.Droid.Friend.Human).height).isWithin(1.72)
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroTypeDependentAliasedField() {
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero).isInstanceOf(HeroTypeDependentAliasedFieldQuery.Data.Hero.DroidHero::class.java)
      assertThat((response.data!!.hero as HeroTypeDependentAliasedFieldQuery.Data.Hero.DroidHero?)?.property).isEqualTo("Astromech")
      true
    }
    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"))
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(HeroTypeDependentAliasedFieldQuery(fromNullable(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero).isInstanceOf(HeroTypeDependentAliasedFieldQuery.Data.Hero.HumanHero::class.java)
      assertThat((response.data!!.hero as HeroTypeDependentAliasedFieldQuery.Data.Hero.HumanHero?)?.property).isEqualTo("Tatooine")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun sameHeroTwice() {
    cacheAndAssertCachedResponse(
        server,
        "SameHeroTwiceResponse.json",
        apolloClient.query(SameHeroTwiceQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.r2?.appearsIn).hasSize(3)
      assertThat(response.data!!.r2?.appearsIn?.get(0)?.name).isEqualTo("NEWHOPE")
      assertThat(response.data!!.r2?.appearsIn?.get(1)?.name).isEqualTo("EMPIRE")
      assertThat(response.data!!.r2?.appearsIn?.get(2)?.name).isEqualTo("JEDI")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailSuccess() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.character).isNotNull()
      assertThat(data.character!!.name).isEqualTo("Han Solo")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailFailIncomplete() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterDetailsQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterDetailsQuery.Data>> { (_, data) ->
          assertThat(data).isNull()
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun independentQueriesGoToNetworkWhenCacheMiss() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response ->
          assertThat(response.hasErrors()).isFalse()
          Truth.assertThat(response.data).isNotNull()
          true
        }
    )
    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(AllPlanetsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.allPlanets).isNotNull()
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun cacheOnlyMissReturnsNullData() {
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { (_, data) -> data == null }
    )
  }

  @Test
  @Throws(Exception::class)
  fun cacheResponseWithNullableFields() {
    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(AllPlanetsQuery()).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        Predicate<Response<AllPlanetsQuery.Data>> { response ->
          Truth.assertThat(response).isNotNull()
          assertThat(response.hasErrors()).isFalse()
          true
        }
    )
    assertResponse(
        apolloClient.query(AllPlanetsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<AllPlanetsQuery.Data>> { response ->
          Truth.assertThat(response).isNotNull()
          assertThat(response.hasErrors()).isFalse()
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun readOperationFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
      assertThat(data.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(data.hero?.friends?.get(1)?.id).isEqualTo("1002")
      assertThat(data.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(data.hero?.friends?.get(2)?.id).isEqualTo("1003")
      assertThat(data.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  // This ttest currently fails because we don't store the typename in HeroAndFriendsNamesWithIDsQuery
  // So we can't query it from HeroWithFriendsFragment
  @Ignore
  @Throws(Exception::class)
  fun readFragmentFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsWithFragmentResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE))),
        Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response -> !response.hasErrors() }
    )
    val heroWithFriendsFragment = apolloClient.apolloStore.readFragment(
        HeroWithFriendsFragmentImpl(),
        from("2001"),
        ).execute()
    assertThat(heroWithFriendsFragment.id).isEqualTo("2001")
    assertThat(heroWithFriendsFragment.name).isEqualTo("R2-D2")
    assertThat(heroWithFriendsFragment.friends).hasSize(3)
    assertThat((heroWithFriendsFragment.friends?.get(0) as HumanWithIdFragment).id).isEqualTo("1000")
    assertThat((heroWithFriendsFragment.friends?.get(0) as HumanWithIdFragment).name).isEqualTo("Luke Skywalker")
    assertThat((heroWithFriendsFragment.friends?.get(1) as HumanWithIdFragment).id).isEqualTo("1002")
    assertThat((heroWithFriendsFragment.friends?.get(1) as HumanWithIdFragment).name).isEqualTo("Han Solo")
    assertThat((heroWithFriendsFragment.friends?.get(2) as HumanWithIdFragment).id).isEqualTo("1003")
    assertThat((heroWithFriendsFragment.friends?.get(2) as HumanWithIdFragment).name).isEqualTo("Leia Organa")

    var fragment: HumanWithIdFragment = apolloClient.apolloStore.readFragment(
        HumanWithIdFragmentImpl(),
        from("1000"),
    ).execute()

    assertThat(fragment.id).isEqualTo("1000")
    assertThat(fragment.name).isEqualTo("Luke Skywalker")

    fragment = apolloClient.apolloStore.readFragment(
        HumanWithIdFragmentImpl(),
        from("1002"),
    ).execute()
    assertThat(fragment.id).isEqualTo("1002")
    assertThat(fragment.name).isEqualTo("Han Solo")

    fragment = apolloClient.apolloStore.readFragment(
        HumanWithIdFragmentImpl(),
        from("1003"),
    ).execute()
    assertThat(fragment.id).isEqualTo("1003")
    assertThat(fragment.name).isEqualTo("Leia Organa")
  }

  @Test
  @Throws(Exception::class)
  fun fromCacheFlag() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response -> !response.isFromCache }
    )
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response -> !response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response -> response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_FIRST),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response -> response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST),
        Predicate<Response<EpisodeHeroNameQuery.Data>> { response -> response.isFromCache }
    )
  }

  @Test
  @Throws(Exception::class)
  fun removeFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
      true
    }

    // test remove root query object
    Truth.assertThat(apolloClient.apolloStore.remove(from("2001")).execute()).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
      true
    }
    )
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
      true
    }

    // test remove object from the list
    Truth.assertThat(apolloClient.apolloStore.remove(from("1002")).execute()).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
      true
    }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      Truth.assertThat(response.data).isNotNull()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun removeMultipleFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Luke Skywalker")
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
      true
    }
    Truth.assertThat(apolloClient.apolloStore.remove(Arrays.asList(from("1002"), from("1000")))
        .execute()).isEqualTo(2)
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirective() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = false)),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = false)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).hasSize(3)
          assertThat(data.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
          assertThat(data.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
          assertThat(data.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
          true
        }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery( episode = Input.fromNullable(Episode.JEDI), includeName = false, skipFriends = false)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { (_, data) ->
          assertThat(data!!.hero?.name).isNull()
          assertThat(data.hero?.friends).hasSize(3)
          assertThat(data.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
          assertThat(data.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
          assertThat(data.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
          true
        }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery( episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = true)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).isNull()
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirectiveUnsatisfiedCache() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = true)),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = true)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery( episode = Input.fromNullable(Episode.JEDI), includeName = true, skipFriends = false)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<HeroAndFriendsDirectivesQuery.Data>> { (_, data) ->
          assertThat(data).isNull()
          true
        }
    )
  }

  @Test
  @Throws(Exception::class)
  fun listOfList() {
    enqueueAndAssertResponse(
        server,
        "StarshipByIdResponse.json",
        apolloClient.query(StarshipByIdQuery("Starship1"))
    ) { (_, data) ->
      assertThat(data!!.starship?.name).isEqualTo("SuperRocket")
      assertThat(data.starship?.coordinates).hasSize(3)
      assertThat(data.starship?.coordinates).containsExactly(Arrays.asList(100.0, 200.0), Arrays.asList(300.0, 400.0),
          Arrays.asList(500.0, 600.0))
      true
    }
    assertResponse(
        apolloClient.query(StarshipByIdQuery("Starship1")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.starship?.name).isEqualTo("SuperRocket")
      assertThat(data.starship?.coordinates).hasSize(3)
      assertThat(data.starship?.coordinates).containsExactly(Arrays.asList(100.0, 200.0), Arrays.asList(300.0, 400.0),
          Arrays.asList(500.0, 600.0))
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun dump() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response -> !response.hasErrors() }
    )
    val dump = apolloClient.apolloStore.normalizedCache().dump()
    Truth.assertThat(prettifyDump(dump)).isEqualTo("""OptimisticNormalizedCache {}
LruNormalizedCache {
  "1002" : {
    "id" : 1002
    "name" : Han Solo
  }

  "QUERY_ROOT" : {
    "hero({"episode":"NEWHOPE"})" : CacheRecordRef(2001)
  }

  "1003" : {
    "id" : 1003
    "name" : Leia Organa
  }

  "1000" : {
    "id" : 1000
    "name" : Luke Skywalker
  }

  "2001" : {
    "id" : 2001
    "name" : R2-D2
    "friends" : [
      CacheRecordRef(1000)
      CacheRecordRef(1002)
      CacheRecordRef(1003)
    ]
  }
}
""")
  }

  @Test
  @Throws(Exception::class)
  fun cascadeRemove() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Luke Skywalker")
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
      true
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
      true
    }

    // test remove root query object
    assertThat(apolloClient.apolloStore.remove(from("2001"), true).execute()).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY), Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>> { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
      true
    }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        Predicate<Response<CharacterNameByIdQuery.Data>> { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
          true
        }
    )
    Truth.assertThat(prettifyDump(apolloClient.apolloStore.normalizedCache().dump())).isEqualTo("""OptimisticNormalizedCache {}
LruNormalizedCache {
  "QUERY_ROOT" : {
    "hero({"episode":"NEWHOPE"})" : CacheRecordRef(2001)
  }
}
""")
  }
}
