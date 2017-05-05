package com.apollographql.apollo;

import com.apollographql.android.impl.httpcache.AllPlanets;
import com.apollographql.android.impl.normalizer.CharacterDetails;
import com.apollographql.android.impl.normalizer.CharacterNameById;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNames;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDForParentOnly;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.HeroAndFriendsWithFragments;
import com.apollographql.android.impl.normalizer.HeroAppearsIn;
import com.apollographql.android.impl.normalizer.HeroParentTypeDependentField;
import com.apollographql.android.impl.normalizer.HeroTypeDependentAliasedField;
import com.apollographql.android.impl.normalizer.SameHeroTwice;
import com.apollographql.android.impl.normalizer.fragment.HeroWithFriendsFragment;
import com.apollographql.android.impl.normalizer.fragment.HumanWithIdFragment;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class NormalizedCacheTestCase {
  private ApolloClient apolloClient;
  private MockWebServer server;

  @Before public void setUp() {
    server = new MockWebServer();

    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

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

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void heroAndFriendsNameResponse() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"));

    HeroAndFriendsNames query = HeroAndFriendsNames.builder().episode(Episode.JEDI).build();

    Response<HeroAndFriendsNames.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().hero().friends()).hasSize(3);
    assertThat(body.data().hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(body.data().hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(body.data().hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void heroAndFriendsNamesWithIDs() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDs query = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();

    Response<HeroAndFriendsNamesWithIDs.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
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

    HeroAndFriendsNamesWithIDForParentOnly query = HeroAndFriendsNamesWithIDForParentOnly.builder()
        .episode(Episode.NEWHOPE).build();

    Response<HeroAndFriendsNamesWithIDForParentOnly.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
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

    HeroAppearsIn query = new HeroAppearsIn();

    Response<HeroAppearsIn.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().appearsIn()).hasSize(3);
    assertThat(body.data().hero().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
    assertThat(body.data().hero().appearsIn().get(1).name()).isEqualTo("EMPIRE");
    assertThat(body.data().hero().appearsIn().get(2).name()).isEqualTo("JEDI");
  }

  @Test public void heroParentTypeDependentField() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroParentTypeDependentFieldDroidResponse.json"));

    HeroParentTypeDependentField query = HeroParentTypeDependentField.builder().episode(Episode.NEWHOPE).build();

    Response<HeroParentTypeDependentField.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
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

    HeroTypeDependentAliasedField query = HeroTypeDependentAliasedField.builder().episode(Episode.NEWHOPE).build();

    Response<HeroTypeDependentAliasedField.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().asHuman()).isNull();
    assertThat(body.data().hero().asDroid().property()).isEqualTo("Astromech");

    server.enqueue(mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json"));

    body = apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().asDroid()).isNull();
    assertThat(body.data().hero().asHuman().property()).isEqualTo("Tatooine");
  }

  @Test public void sameHeroTwice() throws IOException, ApolloException {
    server.enqueue(mockResponse("SameHeroTwiceResponse.json"));

    SameHeroTwice query = new SameHeroTwice();

    Response<SameHeroTwice.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
    assertThat(body.data().r2().appearsIn()).hasSize(3);
    assertThat(body.data().r2().appearsIn().get(0).name()).isEqualTo("NEWHOPE");
    assertThat(body.data().r2().appearsIn().get(1).name()).isEqualTo("EMPIRE");
    assertThat(body.data().r2().appearsIn().get(2).name()).isEqualTo("JEDI");
  }

  @Test public void cacheFirst() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_FIRST).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void cacheOnly() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void networkFirst() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    server.enqueue(mockResponse("HeroNameResponse.json"));
    body = apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_FIRST).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");

    server.enqueue(new MockResponse().setResponseCode(504).setBody(""));
    body = apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_FIRST).execute();
    assertThat(server.getRequestCount()).isEqualTo(3);
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void networkOnly() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();

    server.enqueue(mockResponse("HeroNameResponse.json"));
    body = apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void masterDetailSuccess() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDs query = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();

    CharacterNameById character = CharacterNameById.builder().id("1002").build();
    CharacterNameById.Data characterData = apolloClient.newCall(character).cacheControl(CacheControl.CACHE_ONLY)
        .execute().data();

    assertThat(characterData).isNotNull();
    assertThat(characterData.character()).isNotNull();
    assertThat(characterData.character().asHuman().name()).isEqualTo("Han Solo");
  }

  @Test public void masterDetailFailIncomplete() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDs query = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();

    CharacterDetails character = CharacterDetails.builder().id("1002").build();
    CharacterDetails.Data characterData = apolloClient.newCall(character).cacheControl(CacheControl.CACHE_ONLY)
        .execute().data();

    assertThat(characterData).isNull();
  }

  @Test public void independentQueriesGoToNetworkWhenCacheMiss() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroNameResponse.json"));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).execute();
    assertThat(body.hasErrors()).isFalse();
    assertThat(body.data()).isNotNull();

    server.enqueue(mockResponse("AllPlanetsNullableField.json"));
    AllPlanets allPlanetsQuery = new AllPlanets();
    final Response<AllPlanets.Data> allPlanetsResponse = apolloClient.newCall(allPlanetsQuery).execute();
    assertThat(allPlanetsResponse.hasErrors()).isFalse();
    assertThat(allPlanetsResponse.data().allPlanets()).isNotNull();
  }

  @Test public void cacheOnlyMissReturnsNullData() throws IOException, ApolloException {
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    Response<EpisodeHeroName.Data> body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body.data()).isNull();
  }

  @Test public void cacheResponseWithNullableFields() throws IOException, ApolloException {
    server.enqueue(mockResponse("AllPlanetsNullableField.json"));
    AllPlanets query = new AllPlanets();
    Response<AllPlanets.Data> body = apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();

    assertThat(body).isNotNull();
    assertThat(body.hasErrors()).isFalse();

    body = apolloClient.newCall(query).cacheControl(CacheControl.CACHE_ONLY).execute();
    assertThat(body).isNotNull();
    assertThat(body.hasErrors()).isFalse();
  }

  @Test public void readOperationFromStore() throws IOException, ApolloException {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDs query = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();
    apolloClient.newCall(query).execute();

    HeroAndFriendsNamesWithIDs.Data data = apolloClient.apolloStore().read(query);
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

    HeroAndFriendsNamesWithIDs query = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();
    apolloClient.newCall(query).execute();

    HeroWithFriendsFragment heroWithFriendsFragment = apolloClient.apolloStore().read(
        new HeroWithFriendsFragment.Mapper(), CacheKey.from("2001"), Operation.EMPTY_VARIABLES);

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
        CacheKey.from("1000"), Operation.EMPTY_VARIABLES);
    assertThat(fragment.id()).isEqualTo("1000");
    assertThat(fragment.name()).isEqualTo("Luke Skywalker");

    fragment = apolloClient.apolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1002"),
        Operation.EMPTY_VARIABLES);
    assertThat(fragment.id()).isEqualTo("1002");
    assertThat(fragment.name()).isEqualTo("Han Solo");

    fragment = apolloClient.apolloStore().read(new HumanWithIdFragment.Mapper(), CacheKey.from("1003"),
        Operation.EMPTY_VARIABLES);
    assertThat(fragment.id()).isEqualTo("1003");
    assertThat(fragment.name()).isEqualTo("Leia Organa");
  }

}
