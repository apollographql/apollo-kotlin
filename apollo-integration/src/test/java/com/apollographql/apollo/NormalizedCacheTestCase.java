package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;
import com.apollographql.apollo.integration.normalizer.CharacterDetailsQuery;
import com.apollographql.apollo.integration.normalizer.CharacterNameByIdQuery;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
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

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

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
        .dispatcher(Utils.immediateExecutorService())
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  @Test public void episodeHeroName() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroNameQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void heroAndFriendsNameResponse() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"));

    HeroAndFriendsNamesQuery query = HeroAndFriendsNamesQuery.builder().episode(Episode.JEDI).build();

    Response<HeroAndFriendsNamesQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().friends()).hasSize(3);
    assertThat(body.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(body.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void heroAndFriendsNamesWithIDs() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDsQuery query = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();

    Response<HeroAndFriendsNamesWithIDsQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().id()).isEqualTo("2001");
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().friends()).hasSize(3);
    assertThat(body.data().hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(body.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().friends().get(1).id()).isEqualTo("1002");
    assertThat(body.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(body.data().hero().friends().get(2).id()).isEqualTo("1003");
    assertThat(body.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void heroAndFriendsNameWithIdsForParentOnly() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsParentOnlyResponse.json"));

    HeroAndFriendsNamesWithIDForParentOnlyQuery query = HeroAndFriendsNamesWithIDForParentOnlyQuery.builder()
        .episode(Episode.NEWHOPE).build();

    Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().id()).isEqualTo("2001");
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().friends()).hasSize(3);
    assertThat(body.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(body.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void heroAppearsInResponse() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAppearsInResponse.json"));

    HeroAppearsInQuery query = new HeroAppearsInQuery();

    Response<HeroAppearsInQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().appearsIn()).hasSize(3);
    assertThat(body.data().hero().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
    assertThat(body.data().hero().appearsIn().get(1).name()).isEqualTo("EMPIRE");
    assertThat(body.data().hero().appearsIn().get(2).name()).isEqualTo("JEDI");
  }

  @Test public void heroParentTypeDependentField() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroParentTypeDependentFieldDroidResponse.json"));

    HeroParentTypeDependentFieldQuery query = HeroParentTypeDependentFieldQuery.builder()
        .episode(Episode.NEWHOPE).build();

    Response<HeroParentTypeDependentFieldQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().asDroid().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().asDroid().friends()).hasSize(3);
    assertThat(body.data().hero().asDroid().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().asDroid().friends().get(0).asHuman().name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().asDroid().friends().get(0).asHuman().height()).isWithin(1.72);
  }

  @Test public void heroTypeDependentAliasedField() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponse.json"));

    HeroTypeDependentAliasedFieldQuery query
        = HeroTypeDependentAliasedFieldQuery.builder().episode(Episode.NEWHOPE).build();

    Response<HeroTypeDependentAliasedFieldQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().asHuman()).isNull();
    assertThat(body.data().hero().asDroid().property()).isEqualTo("Astromech");

    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"));

    body = apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().asDroid()).isNull();
    assertThat(body.data().hero().asHuman().property()).isEqualTo("Tatooine");
  }

  @Test public void sameHeroTwice() throws IOException, ApolloException {
    server.enqueue(mockResponse("SameHeroTwiceResponse.json"));

    SameHeroTwiceQuery query = new SameHeroTwiceQuery();

    Response<SameHeroTwiceQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().r2().appearsIn()).hasSize(3);
    assertThat(body.data().r2().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
    assertThat(body.data().r2().appearsIn().get(1).name()).isEqualTo("EMPIRE");
    assertThat(body.data().r2().appearsIn().get(2).name()).isEqualTo("JEDI");
  }

  @Test public void masterDetailSuccess() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDsQuery query = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();

    CharacterNameByIdQuery character = CharacterNameByIdQuery.builder().id("1002").build();
    CharacterNameByIdQuery.Data characterData = apolloClient.query(character).responseFetcher(
        CACHE_ONLY)
        .execute().data();

    assertThat(characterData).isNotNull();
    assertThat(characterData.character()).isNotNull();
    assertThat(characterData.character().asHuman().name()).isEqualTo("Han Solo");
  }

  @Test public void masterDetailFailIncomplete() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDsQuery query = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).execute();

    CharacterDetailsQuery character = CharacterDetailsQuery.builder().id("1002").build();
    CharacterDetailsQuery.Data characterData = apolloClient.query(character).responseFetcher(CACHE_ONLY)
        .execute().data();

    assertThat(characterData).isNull();
  }

  @Test public void independentQueriesGoToNetworkWhenCacheMiss() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    Response<EpisodeHeroNameQuery.Data> body = apolloClient.query(query).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data()).isNotNull();

    server.enqueue(mockResponse("AllPlanetsNullableField.json"));
    AllPlanetsQuery allPlanetsQuery = new AllPlanetsQuery();
    final Response<AllPlanetsQuery.Data> allPlanetsResponse = apolloClient.query(allPlanetsQuery).execute();
    assertThat(allPlanetsResponse.hasErrors()).isFalse();
    assertThat(allPlanetsResponse.data().allPlanets()).isNotNull();
  }

  @Test public void cacheOnlyMissReturnsNullData() throws IOException, ApolloException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    Response<EpisodeHeroNameQuery.Data> body = apolloClient.query(query)
        .responseFetcher(CACHE_ONLY)
        .execute();
    assertThat(body.data()).isNull();
  }

  @Test public void cacheResponseWithNullableFields() throws IOException, ApolloException {
    server.enqueue(mockResponse("AllPlanetsNullableField.json"));
    AllPlanetsQuery query = new AllPlanetsQuery();
    Response<AllPlanetsQuery.Data> body = apolloClient.query(query)
        .responseFetcher(NETWORK_ONLY)
        .execute();

    assertThat(body).isNotNull();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.query(query).responseFetcher(CACHE_ONLY).execute();
    assertThat(body).isNotNull();
    assertThat(body.hasErrors()).isFalse();
  }

  @Test public void readOperationFromStore() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDsQuery query = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();
    apolloClient.query(query).execute();

    HeroAndFriendsNamesWithIDsQuery.Data data = apolloClient.apolloStore().read(query).execute();
    assertThat(data.hero().id()).isEqualTo("2001");
    assertThat(data.hero().name()).isEqualTo("R2-D2");
    assertThat(data.hero().friends()).hasSize(3);
    assertThat(data.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data.hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(data.hero().friends().get(1).id()).isEqualTo("1002");
    assertThat(data.hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(data.hero().friends().get(2).id()).isEqualTo("1003");
    assertThat(data.hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void readFragmentFromStore() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsWithFragmentResponse.json"));

    HeroAndFriendsNamesWithIDsQuery query = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();
    apolloClient.query(query).execute();

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

  @Test public void from_cache_flag() throws Exception {
    server.enqueue(mockResponse("HeroNameResponse.json"));
    assertThat(apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE)).execute().fromCache()).isFalse();

    server.enqueue(mockResponse("HeroNameResponse.json"));
    assertThat(apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE))
        .responseFetcher(NETWORK_ONLY)
        .execute().fromCache()).isFalse();
    assertThat(apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE))
        .responseFetcher(CACHE_ONLY)
        .execute().fromCache()).isTrue();
    assertThat(apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE))
        .responseFetcher(CACHE_FIRST)
        .execute().fromCache()).isTrue();
    assertThat(apolloClient.query(new EpisodeHeroNameQuery(Episode.EMPIRE))
        .responseFetcher(NETWORK_FIRST)
        .execute().fromCache()).isTrue();
  }

  @Test public void remove_from_store() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDsQuery masterQuery = new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE);
    Response<HeroAndFriendsNamesWithIDsQuery.Data> masterQueryResponse = apolloClient.query(masterQuery)
        .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).execute();
    assertThat(masterQueryResponse.data().hero().name()).isEqualTo("R2-D2");
    assertThat(masterQueryResponse.data().hero().friends()).hasSize(3);

    CharacterNameByIdQuery detailQuery = new CharacterNameByIdQuery("1002");
    Response<CharacterNameByIdQuery.Data> detailQueryResponse = apolloClient.query(detailQuery).responseFetcher
        (ApolloResponseFetchers
        .CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Han Solo");

    // test remove root query object
    assertThat(apolloClient.apolloStore().remove(CacheKey.from("2001")).execute()).isTrue();
    masterQueryResponse = apolloClient.query(masterQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(masterQueryResponse.fromCache()).isTrue();
    assertThat(masterQueryResponse.data()).isNull();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    apolloClient.query(masterQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).execute();

    detailQuery = new CharacterNameByIdQuery("1002");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Han Solo");

    // test remove object from the list
    assertThat(apolloClient.apolloStore().remove(CacheKey.from("1002")).execute()).isTrue();

    detailQuery = new CharacterNameByIdQuery("1002");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data()).isNull();
    masterQueryResponse = apolloClient.query(masterQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(masterQueryResponse.fromCache()).isTrue();
    assertThat(masterQueryResponse.data()).isNull();

    detailQuery = new CharacterNameByIdQuery("1003");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data()).isNotNull();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Leia Organa");
  }

  @Test public void remove_multiple_from_store() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDsQuery masterQuery = new HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE);
    Response<HeroAndFriendsNamesWithIDsQuery.Data> masterQueryResponse = apolloClient.query(masterQuery)
        .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).execute();
    assertThat(masterQueryResponse.data().hero().name()).isEqualTo("R2-D2");
    assertThat(masterQueryResponse.data().hero().friends()).hasSize(3);

    CharacterNameByIdQuery detailQuery = new CharacterNameByIdQuery("1000");
    Response<CharacterNameByIdQuery.Data> detailQueryResponse = apolloClient.query(detailQuery)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Luke Skywalker");

    detailQuery = new CharacterNameByIdQuery("1002");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Han Solo");

    detailQuery = new CharacterNameByIdQuery("1003");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Leia Organa");

    assertThat(apolloClient.apolloStore().remove(Arrays.asList(CacheKey.from("1002"), CacheKey.from("1000")))
        .execute()).isEqualTo(2);

    detailQuery = new CharacterNameByIdQuery("1000");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data()).isNull();

    detailQuery = new CharacterNameByIdQuery("1002");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data()).isNull();

    detailQuery = new CharacterNameByIdQuery("1003");
    detailQueryResponse = apolloClient.query(detailQuery).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute();
    assertThat(detailQueryResponse.fromCache()).isTrue();
    assertThat(detailQueryResponse.data().character().asHuman().name()).isEqualTo("Leia Organa");
  }
}
