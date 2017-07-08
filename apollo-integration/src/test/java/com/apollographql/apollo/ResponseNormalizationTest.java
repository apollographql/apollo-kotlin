package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDForParentOnlyQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.HeroAppearsInQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroParentTypeDependentFieldQuery;
import com.apollographql.apollo.integration.normalizer.HeroTypeDependentAliasedFieldQuery;
import com.apollographql.apollo.integration.normalizer.SameHeroTwiceQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.JEDI;
import static com.google.common.truth.Truth.assertThat;

public class ResponseNormalizationTest {
  private ApolloClient apolloClient;
  private MockWebServer server;
  private NormalizedCache normalizedCache;

  private final String QUERY_ROOT_KEY = "QUERY_ROOT";

  @Before public void setUp() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .dispatcher(Utils.immediateExecutorService())
        .build();
    normalizedCache = apolloClient.apolloStore().normalizedCache();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  private MockResponse mockResponse(String fileName) throws IOException, ApolloException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  @Test public void testHeroName() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroNameResponse.json");
    server.enqueue(mockResponse);

    ApolloCall<HeroNameQuery.Data> call = apolloClient.query(new HeroNameQuery());
    Response<HeroNameQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference reference = (CacheReference) record.field("hero");
    assertThat(reference).isEqualTo(new CacheReference("hero"));

    final Record heroRecord = normalizedCache.loadRecord(reference.key(), CacheHeaders.NONE);
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");
  }

  @Test
  public void testHeroNameWithVariable() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("EpisodeHeroNameResponse.json");
    server.enqueue(mockResponse);

    final EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(JEDI).build();
    ApolloCall<EpisodeHeroNameQuery.Data> call = apolloClient.query(query);
    Response<EpisodeHeroNameQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference reference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(reference).isEqualTo(new CacheReference("hero(episode:JEDI)"));

    final Record heroRecord = normalizedCache.loadRecord(reference.key(), CacheHeaders.NONE);
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");
  }


  @Test
  public void testHeroAppearsInQuery() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroAppearsInResponse.json");
    server.enqueue(mockResponse);

    final HeroAppearsInQuery heroAppearsInQuery = new HeroAppearsInQuery();

    ApolloCall<HeroAppearsInQuery.Data> call = apolloClient.query(heroAppearsInQuery);
    Response<HeroAppearsInQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero");
    assertThat(heroReference).isEqualTo(new CacheReference("hero"));

    final Record hero = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(hero.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
  }

  @Test
  public void testHeroAndFriendsNamesQueryWithoutIDs() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNamesQuery heroAndFriendsNameQuery = HeroAndFriendsNamesQuery.builder().episode(JEDI).build();

    ApolloCall<HeroAndFriendsNamesQuery.Data> call = apolloClient.query(heroAndFriendsNameQuery);
    Response<HeroAndFriendsNamesQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("hero(episode:JEDI)"));

    final Record heroRecord = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("hero(episode:JEDI).friends.0"),
        new CacheReference("hero(episode:JEDI).friends.1"),
        new CacheReference("hero(episode:JEDI).friends.2")
    ));

    final Record luke = normalizedCache.loadRecord("hero(episode:JEDI).friends.0", CacheHeaders.NONE);
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testHeroAndFriendsNamesQueryWithIDs() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameWithIdsResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNamesWithIDsQuery heroAndFriendsWithIdsQuery =
        HeroAndFriendsNamesWithIDsQuery.builder().episode(JEDI).build();

    ApolloCall<HeroAndFriendsNamesWithIDsQuery.Data> call = apolloClient.query(heroAndFriendsWithIdsQuery);
    Response<HeroAndFriendsNamesWithIDsQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("2001"));

    final Record heroRecord = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("1000"),
        new CacheReference("1002"),
        new CacheReference("1003")
    ));

    final Record luke = normalizedCache.loadRecord("1000", CacheHeaders.NONE);
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testHeroAndFriendsNamesWithIDForParentOnly() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameWithIdsParentOnlyResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNamesWithIDForParentOnlyQuery heroAndFriendsWithIdsQuery
        = HeroAndFriendsNamesWithIDForParentOnlyQuery.builder().episode(JEDI).build();

    ApolloCall<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data> call = apolloClient.query(heroAndFriendsWithIdsQuery);
    Response<HeroAndFriendsNamesWithIDForParentOnlyQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("2001"));

    final Record heroRecord = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("2001.friends.0"),
        new CacheReference("2001.friends.1"),
        new CacheReference("2001.friends.2")
    ));

    final Record luke = normalizedCache.loadRecord("2001.friends.0", CacheHeaders.NONE);
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testSameHeroTwiceQuery() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("SameHeroTwiceResponse.json");
    server.enqueue(mockResponse);

    final SameHeroTwiceQuery sameHeroTwiceQuery = new SameHeroTwiceQuery();

    ApolloCall<SameHeroTwiceQuery.Data> call = apolloClient.query(sameHeroTwiceQuery);
    Response<SameHeroTwiceQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache
        .loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero");

    final Record hero = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(hero.field("name")).isEqualTo("R2-D2");
    assertThat(hero.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
  }

  @Test
  public void testHeroTypeDependentAliasedFieldQueryDroid() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponse.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedFieldQuery aliasedQuery =
        HeroTypeDependentAliasedFieldQuery.builder().episode(JEDI).build();

    ApolloCall<HeroTypeDependentAliasedFieldQuery.Data> call = apolloClient.query(aliasedQuery);
    Response<HeroTypeDependentAliasedFieldQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");

    final Record hero = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(hero.field("primaryFunction")).isEqualTo("Astromech");
    assertThat(hero.field("__typename")).isEqualTo("Droid");
  }

  @Test
  public void testHeroTypeDependentAliasedFieldQueryHuman() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedFieldQuery aliasedQuery = HeroTypeDependentAliasedFieldQuery.builder().episode(EMPIRE)
        .build();

    ApolloCall<HeroTypeDependentAliasedFieldQuery.Data> call = apolloClient.query(aliasedQuery);
    Response<HeroTypeDependentAliasedFieldQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:EMPIRE)");

    final Record hero = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(hero.field("homePlanet")).isEqualTo("Tatooine");
    assertThat(hero.field("__typename")).isEqualTo("Human");
  }

  @Test
  public void testHeroParentTypeDependentAliasedFieldQueryHuman() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedFieldQuery aliasedQuery = HeroTypeDependentAliasedFieldQuery.builder().episode(EMPIRE)
        .build();

    ApolloCall<HeroTypeDependentAliasedFieldQuery.Data> call = apolloClient.query(aliasedQuery);
    Response<HeroTypeDependentAliasedFieldQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record record = normalizedCache.loadRecord(QUERY_ROOT_KEY, CacheHeaders.NONE);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:EMPIRE)");

    final Record hero = normalizedCache.loadRecord(heroReference.key(), CacheHeaders.NONE);
    assertThat(hero.field("homePlanet")).isEqualTo("Tatooine");
    assertThat(hero.field("__typename")).isEqualTo("Human");
  }

  @Test
  public void testHeroParentTypeDependentFieldDroid() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroParentTypeDependentFieldDroidResponse.json");
    server.enqueue(mockResponse);
    final HeroParentTypeDependentFieldQuery aliasedQuery =
        HeroParentTypeDependentFieldQuery.builder().episode(JEDI).build();

    ApolloCall<HeroParentTypeDependentFieldQuery.Data> call = apolloClient.query(aliasedQuery);
    Response<HeroParentTypeDependentFieldQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record lukeRecord = normalizedCache
        .loadRecord("hero(episode:JEDI).friends.0", CacheHeaders.NONE);
    assertThat(lukeRecord.field("name")).isEqualTo("Luke Skywalker");
    assertThat(lukeRecord.field("height(unit:METER)")).isEqualTo(BigDecimal.valueOf(1.72));

    final List<Object> friends = (List<Object>) normalizedCache
        .loadRecord("hero(episode:JEDI)", CacheHeaders.NONE).field("friends");
    assertThat(friends.get(0)).isEqualTo(new CacheReference("hero(episode:JEDI).friends.0"));
    assertThat(friends.get(1)).isEqualTo(new CacheReference("hero(episode:JEDI).friends.1"));
    assertThat(friends.get(2)).isEqualTo(new CacheReference("hero(episode:JEDI).friends.2"));
  }

  @Test
  public void testHeroParentTypeDependentFieldHuman() throws IOException, ApolloException {
    MockResponse mockResponse = mockResponse("HeroParentTypeDependentFieldHumanResponse.json");
    server.enqueue(mockResponse);
    final HeroParentTypeDependentFieldQuery aliasedQuery =
        HeroParentTypeDependentFieldQuery.builder().episode(EMPIRE).build();

    ApolloCall<HeroParentTypeDependentFieldQuery.Data> call = apolloClient.query(aliasedQuery);
    Response<HeroParentTypeDependentFieldQuery.Data> body = call.execute();
    assertThat(body.hasErrors()).isFalse();

    Record lukeRecord = normalizedCache
        .loadRecord("hero(episode:EMPIRE).friends.0", CacheHeaders.NONE);
    assertThat(lukeRecord.field("name")).isEqualTo("Han Solo");
    assertThat(lukeRecord.field("height(unit:FOOT)")).isEqualTo(BigDecimal.valueOf(5.905512));
  }

}
