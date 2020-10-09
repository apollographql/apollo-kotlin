package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
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
import com.apollographql.apollo.integration.normalizer.StarshipByIdQuery;
import com.apollographql.apollo.integration.normalizer.fragment.HeroWithFriendsFragment;
import com.apollographql.apollo.integration.normalizer.fragment.HumanWithIdFragment;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import io.reactivex.functions.Predicate;
import kotlin.reflect.KClass;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

public class NormalizedCacheTestCase {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .dispatcher(new Dispatcher(Utils.INSTANCE.immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .dispatcher(Utils.INSTANCE.immediateExecutor())
        .build();
  }

  @Test public void episodeHeroName() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNameResponse() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(new HeroAndFriendsNamesQuery(Input.fromNullable(Episode.JEDI))),
        new Predicate<Response<HeroAndFriendsNamesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNamesWithIDs() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().id()).isEqualTo("2001");
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).id()).isEqualTo("1000");
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).id()).isEqualTo("1002");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).id()).isEqualTo("1003");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAndFriendsNameWithIdsForParentOnly() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroAndFriendsNameWithIdsParentOnlyResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDForParentOnlyQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data>>() {
          @Override
          public boolean test(Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().id()).isEqualTo("2001");
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void heroAppearsInResponse() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroAppearsInResponse.json",
        apolloClient.query(new HeroAppearsInQuery()),
        new Predicate<Response<HeroAppearsInQuery.Data>>() {
          @Override
          public boolean test(Response<HeroAppearsInQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().appearsIn()).hasSize(3);
            assertThat(response.getData().hero().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
            assertThat(response.getData().hero().appearsIn().get(1).name()).isEqualTo("EMPIRE");
            assertThat(response.getData().hero().appearsIn().get(2).name()).isEqualTo("JEDI");
            return true;
          }
        }
    );
  }

  @Test public void heroAppearsInResponseWithNulls() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroAppearsInResponseWithNulls.json",
        apolloClient.query(new HeroAppearsInQuery()),
        new Predicate<Response<HeroAppearsInQuery.Data>>() {
          @Override
          public boolean test(Response<HeroAppearsInQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().appearsIn()).hasSize(6);
            assertThat(response.getData().hero().appearsIn().get(0)).isNull();
            assertThat(response.getData().hero().appearsIn().get(1).name()).isEqualTo("NEWHOPE");
            assertThat(response.getData().hero().appearsIn().get(2).name()).isEqualTo("EMPIRE");
            assertThat(response.getData().hero().appearsIn().get(3)).isNull();
            assertThat(response.getData().hero().appearsIn().get(4).name()).isEqualTo("JEDI");
            assertThat(response.getData().hero().appearsIn().get(5)).isNull();
            return true;
          }
        }
    );
  }

  @SuppressWarnings("CheckReturnValue")
  @Test public void heroParentTypeDependentField() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroParentTypeDependentFieldDroidResponse.json",
        apolloClient.query(new HeroParentTypeDependentFieldQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroParentTypeDependentFieldQuery.Data>>() {
          @Override public boolean test(Response<HeroParentTypeDependentFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");

            HeroParentTypeDependentFieldQuery.AsDroid hero = (HeroParentTypeDependentFieldQuery.AsDroid) response.getData().hero();
            assertThat(hero.friends()).hasSize(3);
            assertThat(hero.friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(hero.friends().get(0).name()).isEqualTo("Luke Skywalker");

            assertThat(((HeroParentTypeDependentFieldQuery.AsHuman2) hero.friends().get(0)).height()).isWithin(1.72);
            return true;
          }
        }
    );
  }

  @Test public void heroTypeDependentAliasedField() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(new HeroTypeDependentAliasedFieldQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroTypeDependentAliasedFieldQuery.Data>>() {
          @Override
          public boolean test(Response<HeroTypeDependentAliasedFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero()).isInstanceOf(HeroTypeDependentAliasedFieldQuery.AsDroid.class);
            assertThat(((HeroTypeDependentAliasedFieldQuery.AsDroid) response.getData().hero()).property()).isEqualTo("Astromech");
            return true;
          }
        }
    );
    server.enqueue(Utils.INSTANCE.mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"));
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "HeroTypeDependentAliasedFieldResponse.json",
        apolloClient.query(new HeroTypeDependentAliasedFieldQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroTypeDependentAliasedFieldQuery.Data>>() {
          @Override
          public boolean test(Response<HeroTypeDependentAliasedFieldQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero()).isInstanceOf(HeroTypeDependentAliasedFieldQuery.AsHuman.class);
            assertThat(((HeroTypeDependentAliasedFieldQuery.AsHuman) response.getData().hero()).property()).isEqualTo("Tatooine");
            return true;
          }
        }
    );
  }

  @Test public void sameHeroTwice() throws Exception {
    Utils.INSTANCE.cacheAndAssertCachedResponse(
        server,
        "SameHeroTwiceResponse.json",
        apolloClient.query(new SameHeroTwiceQuery()),
        new Predicate<Response<SameHeroTwiceQuery.Data>>() {
          @Override public boolean test(Response<SameHeroTwiceQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().r2().appearsIn()).hasSize(3);
            assertThat(response.getData().r2().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
            assertThat(response.getData().r2().appearsIn().get(1).name()).isEqualTo("EMPIRE");
            assertThat(response.getData().r2().appearsIn().get(2).name()).isEqualTo("JEDI");
            return true;
          }
        }
    );
  }

  @Test public void masterDetailSuccess() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.getData().character()).isNotNull();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );
  }

  @Test public void masterDetailFailIncomplete() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterDetailsQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterDetailsQuery.Data>>() {
          @Override public boolean test(Response<CharacterDetailsQuery.Data> response) throws Exception {
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );
  }


  @Test public void independentQueriesGoToNetworkWhenCacheMiss() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData()).isNotNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "AllPlanetsNullableField.json",
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.getData().allPlanets()).isNotNull();
            return true;
          }
        }
    );
  }

  @Test public void cacheOnlyMissReturnsNullData() throws Exception {
    Utils.INSTANCE.assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).responseFetcher(CACHE_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.getData() == null;
          }
        }
    );
  }

  @Test public void cacheResponseWithNullableFields() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
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

    Utils.INSTANCE.assertResponse(
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
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().id()).isEqualTo("2001");
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).id()).isEqualTo("1000");
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).id()).isEqualTo("1002");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).id()).isEqualTo("1003");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void readFragmentFromStore() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsWithFragmentResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE))),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    HeroWithFriendsFragment heroWithFriendsFragment = apolloClient.getApolloStore().read(
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

    HumanWithIdFragment fragment = apolloClient.getApolloStore().read(new HumanWithIdFragment.Mapper(),
        CacheKey.from("1000"), Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1000");
    assertThat(fragment.name()).isEqualTo("Luke Skywalker");

    fragment = apolloClient.getApolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1002"),
        Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1002");
    assertThat(fragment.name()).isEqualTo("Han Solo");

    fragment = apolloClient.getApolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1003"),
        Operation.EMPTY_VARIABLES).execute();
    assertThat(fragment.id()).isEqualTo("1003");
    assertThat(fragment.name()).isEqualTo("Leia Organa");
  }

  @Test public void fromCacheFlag() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.isFromCache();
          }
        }
    );

    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.isFromCache();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).responseFetcher(CACHE_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.isFromCache();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).responseFetcher(CACHE_FIRST),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.isFromCache();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).responseFetcher(NETWORK_FIRST),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.isFromCache();
          }
        }
    );
  }

  @Test public void removeFromStore() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    // test remove root query object
    assertThat(apolloClient.getApolloStore().remove(CacheKey.from("2001")).execute()).isTrue();

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(CACHE_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    // test remove object from the list
    assertThat(apolloClient.getApolloStore().remove(CacheKey.from("1002")).execute()).isTrue();

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(CACHE_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void removeMultipleFromStore() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Luke Skywalker");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    assertThat(apolloClient.getApolloStore().remove(asList(CacheKey.from("1002"), CacheKey.from("1000")))
        .execute()).isEqualTo(2);

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );
  }

  @Test public void skipIncludeDirective() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(false).build()),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(false).build()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(false).skipFriends(false).build()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isNull();
            assertThat(response.getData().hero().friends()).hasSize(3);
            assertThat(response.getData().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
            assertThat(response.getData().hero().friends().get(1).name()).isEqualTo("Han Solo");
            assertThat(response.getData().hero().friends().get(2).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(true).build()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).isNull();
            return true;
          }
        }
    );
  }

  @Test public void skipIncludeDirectiveUnsatisfiedCache() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroNameResponse.json",
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(true).build()),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(true).build()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(HeroAndFriendsDirectivesQuery.builder().episode(Episode.JEDI).includeName(true).skipFriends(false).build()).responseFetcher(CACHE_ONLY),
        new Predicate<Response<HeroAndFriendsDirectivesQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsDirectivesQuery.Data> response) throws Exception {
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );
  }

  @Test public void listOfList() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "StarshipByIdResponse.json",
        apolloClient.query(new StarshipByIdQuery("Starship1")),
        new Predicate<Response<StarshipByIdQuery.Data>>() {
          @Override public boolean test(Response<StarshipByIdQuery.Data> response) throws Exception {
            assertThat(response.getData().starship().__typename()).isEqualTo("Starship");
            assertThat(response.getData().starship().name()).isEqualTo("SuperRocket");
            assertThat(response.getData().starship().coordinates()).hasSize(3);
            assertThat(response.getData().starship().coordinates()).containsExactly(asList(100d, 200d), asList(300d, 400d),
                asList(500d, 600d));
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new StarshipByIdQuery("Starship1")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<StarshipByIdQuery.Data>>() {
          @Override public boolean test(Response<StarshipByIdQuery.Data> response) throws Exception {
            assertThat(response.getData().starship().__typename()).isEqualTo("Starship");
            assertThat(response.getData().starship().name()).isEqualTo("SuperRocket");
            assertThat(response.getData().starship().coordinates()).hasSize(3);
            assertThat(response.getData().starship().coordinates()).containsExactly(asList(100d, 200d), asList(300d, 400d),
                asList(500d, 600d));
            return true;
          }
        }
    );
  }

  @Test public void dump() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    Map<KClass<?>, Map<String, Record>> dump = apolloClient.getApolloStore().normalizedCache().dump();
    assertThat(NormalizedCache.prettifyDump(dump)).isEqualTo("OptimisticNormalizedCache {}\n" +
        "LruNormalizedCache {\n" +
        "  \"1002\" : {\n" +
        "    \"__typename\" : Human\n" +
        "    \"id\" : 1002\n" +
        "    \"name\" : Han Solo\n" +
        "  }\n" +
        "\n" +
        "  \"QUERY_ROOT\" : {\n" +
        "    \"hero({\"episode\":\"NEWHOPE\"})\" : CacheRecordRef(2001)\n" +
        "  }\n" +
        "\n" +
        "  \"1003\" : {\n" +
        "    \"__typename\" : Human\n" +
        "    \"id\" : 1003\n" +
        "    \"name\" : Leia Organa\n" +
        "  }\n" +
        "\n" +
        "  \"1000\" : {\n" +
        "    \"__typename\" : Human\n" +
        "    \"id\" : 1000\n" +
        "    \"name\" : Luke Skywalker\n" +
        "  }\n" +
        "\n" +
        "  \"2001\" : {\n" +
        "    \"__typename\" : Droid\n" +
        "    \"id\" : 2001\n" +
        "    \"name\" : R2-D2\n" +
        "    \"friends\" : [\n" +
        "      CacheRecordRef(1000)\n" +
        "      CacheRecordRef(1002)\n" +
        "      CacheRecordRef(1003)\n" +
        "    ]\n" +
        "  }\n" +
        "}\n");
  }

  @Test public void cascadeRemove() throws Exception {
    Utils.INSTANCE.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(NETWORK_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.getData().hero().name()).isEqualTo("R2-D2");
            assertThat(response.getData().hero().friends()).hasSize(3);
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Luke Skywalker");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Han Solo");
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(((CharacterNameByIdQuery.AsHuman) response.getData().character()).name()).isEqualTo("Leia Organa");
            return true;
          }
        }
    );

    // test remove root query object
    assertThat(apolloClient.getApolloStore().remove(CacheKey.from("2001"), true).execute()).isTrue();

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
            .responseFetcher(CACHE_ONLY), new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1000")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1002")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    Utils.INSTANCE.assertResponse(
        apolloClient.query(new CharacterNameByIdQuery("1003")).responseFetcher(CACHE_ONLY),
        new Predicate<Response<CharacterNameByIdQuery.Data>>() {
          @Override public boolean test(Response<CharacterNameByIdQuery.Data> response) throws Exception {
            assertThat(response.isFromCache()).isTrue();
            assertThat(response.getData()).isNull();
            return true;
          }
        }
    );

    assertThat(NormalizedCache.prettifyDump(apolloClient.getApolloStore().normalizedCache().dump())).isEqualTo("" +
        "OptimisticNormalizedCache {}\n" +
        "LruNormalizedCache {\n" +
        "  \"QUERY_ROOT\" : {\n" +
        "    \"hero({\"episode\":\"NEWHOPE\"})\" : CacheRecordRef(2001)\n" +
        "  }\n" +
        "}\n");
  }
}
