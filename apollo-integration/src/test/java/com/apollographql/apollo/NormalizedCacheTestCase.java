package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;
import com.apollographql.apollo.integration.normalizer.CharacterDetailsQuery;
import com.apollographql.apollo.integration.normalizer.CharacterNameByIdQuery;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsDirectivesQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDForParentOnlyQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.HeroAppearsInQuery;
import com.apollographql.apollo.integration.normalizer.HeroParentTypeDependentFieldQuery;
import com.apollographql.apollo.integration.normalizer.HeroTypeDependentAliasedFieldQuery;
import com.apollographql.apollo.integration.normalizer.SameHeroTwiceQuery;
import com.apollographql.apollo.integration.normalizer.fragment.HeroWithFriendsFragment;
import com.apollographql.apollo.integration.normalizer.fragment.HumanWithIdFragment;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.cacheAndAssertCachedResponse;
import static com.apollographql.apollo.Utils.assertResponse;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;

public class NormalizedCacheTestCase {
  private ApolloClient apolloClient;
  private MockWebServer server;

  @Before public void setUp() {
    server = new MockWebServer();

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .dispatcher(Utils.immediateExecutor())
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  @Test public void episodeHeroName() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNameResponse() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(new HeroAndFriendsNamesQuery(Episode.JEDI)),
        new Predicate<Response<HeroAndFriendsNamesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNamesWithIDs() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().id()).isEqualTo("2001");
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).id()).isEqualTo("1000");
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).id()).isEqualTo("1002");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).id()).isEqualTo("1003");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNameWithIdsForParentOnly() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsParentOnlyResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDForParentOnlyQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data>>() {
          @Override
          public boolean test(Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().id()).isEqualTo("2001");
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAppearsInResponse() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroAppearsInResponse.json",
        apolloClient.query(new HeroAppearsInQuery()),
        new Predicate<Response<HeroAppearsInQuery.Data>>() {
          @Override
          public boolean test(Response<HeroAppearsInQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().appearsIn()).hasSize(3);
            assertThat(response.data().hero().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
            assertThat(response.data().hero().appearsIn().get(1).name()).isEqualTo("EMPIRE");
            assertThat(response.data().hero().appearsIn().get(2).name()).isEqualTo("JEDI");
            return true;
          }
        }
    );
  }

  @Test public void heroParentTypeDependentField() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroParentTypeDependentFieldDroidResponse.json",
        apolloClient.query(new HeroParentTypeDependentFieldQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroParentTypeDependentFieldQuery.Data>>() {
          @Override public boolean test(Response<HeroParentTypeDependentFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().asDroid().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().asDroid().friends()).hasSize(3);
            assertThat(response.data().hero().asDroid().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().asDroid().friends().get(0).asHuman().name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().asDroid().friends().get(0).asHuman().height()).isWithin(1.72);
            return true;
          }
        }
    );
  }

  @Test public void heroTypeDependentAliasedField() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(new HeroTypeDependentAliasedFieldQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroTypeDependentAliasedFieldQuery.Data>>() {
          @Override
          public boolean test(Response<HeroTypeDependentAliasedFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().asHuman()).isNull();
            assertThat(response.data().hero().asDroid().property()).isEqualTo("Astromech");
            return true;
          }
        }
    );
    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"));
    cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(new HeroTypeDependentAliasedFieldQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroTypeDependentAliasedFieldQuery.Data>>() {
          @Override
          public boolean test(Response<HeroTypeDependentAliasedFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().asDroid()).isNull();
            assertThat(response.data().hero().asHuman().property()).isEqualTo("Tatooine");
            return true;
          }
        }
    );
  }

  @Test public void sameHeroTwice() throws Exception {
    cacheAndAssertCachedResponse(
        server,
        "SameHeroTwiceResponse.json",
        apolloClient.query(new SameHeroTwiceQuery()),
        new Predicate<Response<SameHeroTwiceQuery.Data>>() {
          @Override public boolean test(Response<SameHeroTwiceQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().r2().appearsIn()).hasSize(3);
            assertThat(response.data().r2().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
            assertThat(response.data().r2().appearsIn().get(1).name()).isEqualTo("EMPIRE");
            assertThat(response.data().r2().appearsIn().get(2).name()).isEqualTo("JEDI");
            return true;
          }
        }
    );
  }

  @Test public void masterDetailSuccess() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.data().character()).isNotNull();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );
  }

  @Test public void masterDetailFailIncomplete() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterDetailsQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterDetailsQuery.Data>>() {
          @Override public boolean test(Response<CharacterDetailsQuery.Data> response) throws Exception {
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );
  }


  @Test public void independentQueriesGoToNetworkWhenCacheMiss() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data()).isNotNull();
            return true;
          }
        }
    );

    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().allPlanets()).isNotNull();
            return true;
          }
        }
    );
  }

  @Test public void cacheOnlyMissReturnsNullData() throws Exception {
    assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data() == null;
          }
        }
    );
  }

  @Test public void cacheResponseWithNullableFields() throws Exception {
    enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(new AllPlanetsQuery()).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response).isNotNull();
            assertThat(response.hasErrors()).isFalse();
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new AllPlanetsQuery()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response).isNotNull();
            assertThat(response.hasErrors()).isFalse();
            return true;
          }
        }
    );
  }

  @Test public void readOperationFromStore() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.data().hero().id()).isEqualTo("2001");
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).id()).isEqualTo("1000");
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).id()).isEqualTo("1002");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).id()).isEqualTo("1003");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void readFragmentFromStore() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsWithFragmentResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    HeroWithFriendsFragment heroWithFriendsFragment = apolloClient.apolloStore().read(
        new HeroWithFriendsFragment.Mapper(), CacheKey.from("2001"), Operation.EMPTY_VARIABLES).execute();

    assertThat(heroWithFriendsFragment.id()).isEqualTo("2001");
    assertThat(heroWithFriendsFragment.name()).isEqualTo("R2-D2");
    assertThat(heroWithFriendsFragment.friends()).hasSize(3);
    assertThat(heroWithFriendsFragment.friends().get(0).fragments().humanWithIdFragment().id()).isEqualTo("1000");
    assertThat(heroWithFriendsFragment.friends().get(0).fragments().humanWithIdFragment().name()).isEqualTo("Luke Skywalker");
    assertThat(heroWithFriendsFragment.friends().get(1).fragments().humanWithIdFragment().id()).isEqualTo("1002");
    assertThat(heroWithFriendsFragment.friends().get(1).fragments().humanWithIdFragment().name()).isEqualTo("Han Solo");
    assertThat(heroWithFriendsFragment.friends().get(2).fragments().humanWithIdFragment().id()).isEqualTo("1003");
    assertThat(heroWithFriendsFragment.friends().get(2).fragments().humanWithIdFragment().name()).isEqualTo("Leia Organa");

    HumanWithIdFragment fragment = apolloClient.apolloStore().read(new HumanWithIdFragment.Mapper(),
        CacheKey.from("1000"), Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1000");
    assertThat(fragment.name()).isEqualTo("Luke Skywalker");

    fragment = apolloClient.apolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1002"),
        Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1002");
    assertThat(fragment.name()).isEqualTo("Han Solo");

    fragment = apolloClient.apolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1003"),
        Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1003");
    assertThat(fragment.name()).isEqualTo("Leia Organa");
  }

  @Test public void fromCacheFlag() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.fromCache();
          }
        }
    );

    enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.fromCache();
          }
        }
    );

    assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.fromCache();
          }
        }
    );

    assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).responseFetcher(CACHE_FIRST),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.fromCache();
          }
        }
    );

    assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).responseFetcher(NETWORK_FIRST),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.fromCache();
          }
        }
    );
  }

  @Test public void removeFromStore() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    // test remove root query object
    assertThat(apolloClient.apolloStore().remove(CacheKey.from("2001")).execute()).isTrue();

    assertResponse(
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );

    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    // test remove object from the list
    assertThat(apolloClient.apolloStore().remove(CacheKey.from("1002")).execute()).isTrue();

    assertResponse(
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void removeMultipleFromStore() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Luke Skywalker");
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    assertThat(apolloClient.apolloStore().remove(Arrays.asList(CacheKey.from("1002"), CacheKey.from("1000")))
        .execute()).isEqualTo(2);

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data()).isNull();
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.fromCache()).isTrue();
            assertThat(response.data().character().asHuman().name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void skipIncludeDirective() throws Exception {
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(new HeroAndFriendsNamesQuery(Episode.JEDI)),
        new Predicate<Response<HeroAndFriendsNamesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(false).build()),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(false).skipFriends(false).build()),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isNull();
            assertThat(response.data().hero().friends()).hasSize(3);
            assertThat(response.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(true).build()),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.data().hero().friends()).isNull();
            return true;
          }
        }
    );
  }
}
