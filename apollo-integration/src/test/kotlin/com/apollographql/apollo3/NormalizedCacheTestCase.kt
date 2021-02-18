package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.assertResponse
import com.apollographql.apollo3.Utils.cacheAndAssertCachedResponse
import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.mockResponse
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.cache.normalized.CacheKey.Companion.from
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.NormalizedCache.Companion.prettifyDump
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.CharacterDetailsQuery
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsDirectivesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDForParentOnlyQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroAppearsInQuery
import com.apollographql.apollo3.integration.normalizer.HeroParentTypeDependentFieldQuery
import com.apollographql.apollo3.integration.normalizer.HeroTypeDependentAliasedFieldQuery
import com.apollographql.apollo3.integration.normalizer.SameHeroTwiceQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.fragment.HeroWithFriendsFragmentImpl
import com.apollographql.apollo3.integration.normalizer.fragment.HumanWithIdFragment
import com.apollographql.apollo3.integration.normalizer.fragment.HumanWithIdFragmentImpl
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.Arrays
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
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun episodeHeroName() {
    cacheAndAssertCachedResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNameResponse() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(HeroAndFriendsNamesQuery(Input.present(Episode.JEDI)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.friends).hasSize(3)
      assertThat(response.data!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(response.data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(response.data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNamesWithIDs() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
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
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroAndFriendsNameWithIdsForParentOnly() {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsParentOnlyResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDForParentOnlyQuery(Input.present(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.id).isEqualTo("2001")
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.friends).hasSize(3)
      assertThat(response.data!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(response.data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(response.data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
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
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroParentTypeDependentField() {
    cacheAndAssertCachedResponse(
        server,
        "HeroParentTypeDependentFieldDroidResponse.json",
        apolloClient.query(HeroParentTypeDependentFieldQuery(Input.present(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(response.data!!.hero?.name).isEqualTo("R2-D2")
      val hero = response.data!!.hero as HeroParentTypeDependentFieldQuery.Data.Hero.DroidHero
      assertThat(hero.friends).hasSize(3)
      assertThat(hero.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(hero.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat((hero.friends?.get(0) as HeroParentTypeDependentFieldQuery.Data.Hero.DroidHero.Friends.HumanFriends).height).isWithin(1.72)
    }
  }

  @Test
  @Throws(Exception::class)
  fun heroTypeDependentAliasedField() {
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(HeroTypeDependentAliasedFieldQuery(Input.present(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero).isInstanceOf(HeroTypeDependentAliasedFieldQuery.Data.Hero.DroidHero::class.java)
      assertThat((response.data!!.hero as HeroTypeDependentAliasedFieldQuery.Data.Hero.DroidHero?)?.property).isEqualTo("Astromech")
    }
    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"))
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(HeroTypeDependentAliasedFieldQuery(Input.present(Episode.NEWHOPE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.hero).isInstanceOf(HeroTypeDependentAliasedFieldQuery.Data.Hero.HumanHero::class.java)
      assertThat((response.data!!.hero as HeroTypeDependentAliasedFieldQuery.Data.Hero.HumanHero?)?.property).isEqualTo("Tatooine")
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
    }
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailSuccess() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY), 
         { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.character).isNotNull()
      assertThat(data.character!!.name).isEqualTo("Han Solo")
    }
  }

  @Test
  @Throws(Exception::class)
  fun masterDetailFailIncomplete() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),  { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterDetailsQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data).isNull()
            }
    )
  }

  @Test
  @Throws(Exception::class)
  fun independentQueriesGoToNetworkWhenCacheMiss() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      Truth.assertThat(response.data).isNotNull()
    }
    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(AllPlanetsQuery())
    ) { response ->
      assertThat(response.hasErrors()).isFalse()
      assertThat(response.data!!.allPlanets).isNotNull()
    }
  }

  @Test
  @Throws(Exception::class)
  fun cacheOnlyMissReturnsNullData() {
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
         { (_, data) -> data == null }
    )
  }

  @Test
  @Throws(Exception::class)
  fun cacheResponseWithNullableFields() {
    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(AllPlanetsQuery()).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { response ->
      assertThat(response).isNotNull()
      assertThat(response.hasErrors()).isFalse()
    }
    assertResponse(
        apolloClient.query(AllPlanetsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response).isNotNull()
      assertThat(response.hasErrors()).isFalse()
    }
  }

  @Test
  @Throws(Exception::class)
  fun readOperationFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
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
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE))),
         { response -> !response.hasErrors() }
    )
    val heroWithFriendsFragment = apolloClient.apolloStore.readFragment(
        HeroWithFriendsFragmentImpl(),
        from("2001"),
    )!!
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
    )!!

    assertThat(fragment.id).isEqualTo("1000")
    assertThat(fragment.name).isEqualTo("Luke Skywalker")

    fragment = apolloClient.apolloStore.readFragment(
        HumanWithIdFragmentImpl(),
        from("1002"),
    )!!
    assertThat(fragment.id).isEqualTo("1002")
    assertThat(fragment.name).isEqualTo("Han Solo")

    fragment = apolloClient.apolloStore.readFragment(
        HumanWithIdFragmentImpl(),
        from("1003"),
    )!!
    assertThat(fragment.id).isEqualTo("1003")
    assertThat(fragment.name).isEqualTo("Leia Organa")
  }

  @Test
  @Throws(Exception::class)
  fun fromCacheFlag() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))),
         { response -> !response.isFromCache }
    )
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
         { response -> !response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
         { response -> response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.CACHE_FIRST),
         { response -> response.isFromCache }
    )
    assertResponse(
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST),
         { response -> response.isFromCache }
    )
  }

  @Test
  @Throws(Exception::class)
  fun removeFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
    }

    // test remove root query object
    assertThat(apolloClient.apolloStore.remove(from("2001"))).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),  { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
    }

    // test remove object from the list
    assertThat(apolloClient.apolloStore.remove(from("1002"))).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
            }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      Truth.assertThat(response.data).isNotNull()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
    }
  }

  @Test
  @Throws(Exception::class)
  fun removeMultipleFromStore() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Luke Skywalker")
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
    }
    Truth.assertThat(apolloClient.apolloStore.remove(Arrays.asList(from("1002"), from("1000")))).isEqualTo(2)
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
            }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { response ->
          assertThat(response.isFromCache).isTrue()
          assertThat(response.data).isNull()
            }
    )
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
    }
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirective() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI), includeName = true, skipFriends = false))
    ) { response -> !response.hasErrors() }

    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI), includeName = true, skipFriends = false)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).hasSize(3)
          assertThat(data.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
          assertThat(data.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
          assertThat(data.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
            }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI), includeName = false, skipFriends = false)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data!!.hero?.name).isNull()
          assertThat(data.hero?.friends).hasSize(3)
          assertThat(data.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
          assertThat(data.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
          assertThat(data.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
            }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI), includeName = true, skipFriends = true)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).isNull()
            }
    )
  }

  @Test
  @Throws(Exception::class)
  fun skipIncludeDirectiveUnsatisfiedCache() {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI),
            includeName = true,
            skipFriends = true))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { response -> !response.hasErrors() }
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI),
            includeName = true,
            skipFriends = true))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data!!.hero?.name).isEqualTo("R2-D2")
          assertThat(data.hero?.friends).isNull()
            }
    )
    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery(episode = Input.present(Episode.JEDI),
            includeName = true,
            skipFriends = false))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY),
        { (_, data) ->
          assertThat(data).isNull()
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
    }
    assertResponse(
        apolloClient.query(StarshipByIdQuery("Starship1")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.starship?.name).isEqualTo("SuperRocket")
      assertThat(data.starship?.coordinates).hasSize(3)
      assertThat(data.starship?.coordinates).containsExactly(Arrays.asList(100.0, 200.0), Arrays.asList(300.0, 400.0),
          Arrays.asList(500.0, 600.0))
    }
  }

  @Test
  @Throws(Exception::class)
  fun dump() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),  { response -> !response.hasErrors() }
    )
    val dump = apolloClient.apolloStore.normalizedCache().dump()
    Truth.assertThat(prettifyDump(dump)).isEqualTo(
        """
          OptimisticCache {}
          MemoryCache {
            "1000" : {
              "id" : 1000
              "name" : Luke Skywalker
            }
          
            "1002" : {
              "id" : 1002
              "name" : Han Solo
            }
          
            "1003" : {
              "id" : 1003
              "name" : Leia Organa
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
          
            "QUERY_ROOT" : {
              "hero({"episode":"NEWHOPE"})" : CacheRecordRef(2001)
            }
          }

        """.trimIndent()
    )
  }

  @Test
  @Throws(Exception::class)
  fun cascadeRemove() {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data.hero?.friends).hasSize(3)
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Luke Skywalker")
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Han Solo")
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data!!.character!!.name).isEqualTo("Leia Organa")
    }

    // test remove root query object
    assertThat(apolloClient.apolloStore.remove(from("2001"), true)).isTrue()
    assertResponse(
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1000")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    assertResponse(
        apolloClient.query(CharacterNameByIdQuery("1002")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    assertResponse(apolloClient.query(CharacterNameByIdQuery("1003")).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)) { response ->
      assertThat(response.isFromCache).isTrue()
      assertThat(response.data).isNull()
    }
    assertThat(prettifyDump(apolloClient.apolloStore.normalizedCache().dump())).isEqualTo("""OptimisticCache {}
MemoryCache {
  "QUERY_ROOT" : {
    "hero({"episode":"NEWHOPE"})" : CacheRecordRef(2001)
  }
}
""")
  }
}
