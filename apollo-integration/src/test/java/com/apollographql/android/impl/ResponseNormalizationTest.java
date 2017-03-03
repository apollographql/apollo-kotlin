package com.apollographql.android.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.cache.normalized.CacheReference;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNames;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDForParentOnly;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.HeroAppearsIn;
import com.apollographql.android.impl.normalizer.HeroName;
import com.apollographql.android.impl.normalizer.HeroTypeDependentAliasedField;
import com.apollographql.android.impl.normalizer.SameHeroTwice;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.android.impl.normalizer.type.Episode.EMPIRE;
import static com.apollographql.android.impl.normalizer.type.Episode.JEDI;
import static com.google.common.truth.Truth.assertThat;

public class ResponseNormalizationTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

  private ApolloClient apolloClient;
  private MockWebServer server;
  private InMemoryCacheStore cacheStore;

  private final String QUERY_ROOT_KEY = "QUERY_ROOT";
  private static final String NORMALIZER_TEST_PATH  = "src/test/graphql/com/apollographql/android/impl/normalizer/";

  @Before public void setUp() {
    server = new MockWebServer();
    CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(String value) {
        try {
          return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public String encode(Date value) {
        return DATE_FORMAT.format(value);
      }
    };

    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    cacheStore = new InMemoryCacheStore();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(cacheStore, new CacheKeyResolver() {
          @Nullable @Override public String resolve(Map<String, Object> jsonObject) {
            return (String) jsonObject.get("id");
          }
        })
        .build();
  }

  @Test public void testHeroName() throws IOException {
    MockResponse mockResponse = mockResponse("HeroNameResponse.json");
    server.enqueue(mockResponse);

    ApolloCall<HeroName.Data> call = apolloClient.newCall(new HeroName());
    Response<HeroName.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference reference = (CacheReference) record.field("hero");
    assertThat(reference).isEqualTo(new CacheReference("hero"));

    final Record heroRecord = cacheStore.loadRecord(reference.key());
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");
  }

  @Test
  public void testHeroNameWithVariable() throws IOException {
    MockResponse mockResponse = mockResponse("EpisodeHeroNameResponse.json");
    server.enqueue(mockResponse);

    final EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(JEDI).build());
    ApolloCall<EpisodeHeroName.Data> call = apolloClient.newCall(query);
    Response<EpisodeHeroName.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference reference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(reference).isEqualTo(new CacheReference("hero(episode:JEDI)"));

    final Record heroRecord = cacheStore.loadRecord(reference.key());
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");
  }

  private static MockResponse mockResponse(String normalizerFileName) throws IOException {
    return new MockResponse().setChunkedBody(Files.toString(new File(NORMALIZER_TEST_PATH + normalizerFileName),
        Charsets.UTF_8), 32);
  }

  @Test
  public void testHeroAppearsInQuery() throws IOException {
    MockResponse mockResponse = mockResponse("HeroAppearsInResponse.json");
    server.enqueue(mockResponse);

    final HeroAppearsIn heroAppearsInQuery = new HeroAppearsIn();

    ApolloCall<HeroAppearsIn.Data> call = apolloClient.newCall(heroAppearsInQuery);
    Response<HeroAppearsIn.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero");
    assertThat(heroReference).isEqualTo(new CacheReference("hero"));

    final Record hero = cacheStore.loadRecord(heroReference.key());
    assertThat(hero.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
  }

  @Test
  public void testHeroAndFriendsNamesQueryWithoutIDs() throws IOException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNames heroAndFriendsNameQuery
        = new HeroAndFriendsNames(HeroAndFriendsNames.Variables.builder().episode(JEDI).build());

    ApolloCall<HeroAndFriendsNames.Data> call = apolloClient.newCall(heroAndFriendsNameQuery);
    Response<HeroAndFriendsNames.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("hero(episode:JEDI)"));

    final Record heroRecord = cacheStore.loadRecord(heroReference.key());
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("hero(episode:JEDI).friends.0"),
        new CacheReference("hero(episode:JEDI).friends.1"),
        new CacheReference("hero(episode:JEDI).friends.2")
    ));

    final Record luke = cacheStore.loadRecord("hero(episode:JEDI).friends.0");
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testHeroAndFriendsNamesQueryWithIDs() throws IOException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameWithIdsResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNamesWithIDs heroAndFriendsWithIdsQuery
        = new HeroAndFriendsNamesWithIDs(HeroAndFriendsNamesWithIDs.Variables.builder().episode(JEDI).build());

    ApolloCall<HeroAndFriendsNamesWithIDs.Data> call = apolloClient.newCall(heroAndFriendsWithIdsQuery);
    Response<HeroAndFriendsNamesWithIDs.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("2001"));

    final Record heroRecord = cacheStore.loadRecord(heroReference.key());
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("1000"),
        new CacheReference("1002"),
        new CacheReference("1003")
    ));

    final Record luke = cacheStore.loadRecord("1000");
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testHeroAndFriendsNamesWithIDForParentOnly() throws IOException {
    MockResponse mockResponse = mockResponse("HeroAndFriendsNameWithIdsParentOnlyResponse.json");
    server.enqueue(mockResponse);
    final HeroAndFriendsNamesWithIDForParentOnly heroAndFriendsWithIdsQuery =
        new HeroAndFriendsNamesWithIDForParentOnly(
            HeroAndFriendsNamesWithIDForParentOnly.Variables.builder().episode(JEDI).build()
        );

    ApolloCall<HeroAndFriendsNamesWithIDForParentOnly.Data> call = apolloClient.newCall(heroAndFriendsWithIdsQuery);
    Response<HeroAndFriendsNamesWithIDForParentOnly.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");
    assertThat(heroReference).isEqualTo(new CacheReference("2001"));

    final Record heroRecord = cacheStore.loadRecord(heroReference.key());
    assertThat(heroRecord.field("name")).isEqualTo("R2-D2");

    assertThat(heroRecord.field("friends")).isEqualTo(Arrays.asList(
        new CacheReference("2001.friends.0"),
        new CacheReference("2001.friends.1"),
        new CacheReference("2001.friends.2")
    ));

    final Record luke = cacheStore.loadRecord("2001.friends.0");
    assertThat(luke.field("name")).isEqualTo("Luke Skywalker");
  }

  @Test
  public void testSameHeroTwiceQuery() throws IOException {
    MockResponse mockResponse = mockResponse("SameHeroTwiceResponse.json");
    server.enqueue(mockResponse);

    final SameHeroTwice sameHeroTwiceQuery = new SameHeroTwice();

    ApolloCall<SameHeroTwice.Data> call = apolloClient.newCall(sameHeroTwiceQuery);
    Response<SameHeroTwice.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero");

    final Record hero = cacheStore.loadRecord(heroReference.key());
    assertThat(hero.field("name")).isEqualTo("R2-D2");
    assertThat(hero.field("appearsIn")).isEqualTo(Arrays.asList("NEWHOPE", "EMPIRE", "JEDI"));
  }

  @Test
  public void testHeroTypeDependentAliasedFieldQueryDroid() throws IOException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponse.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedField aliasedQuery = new HeroTypeDependentAliasedField(
        HeroTypeDependentAliasedField.Variables.builder().episode(JEDI).build()
    );

    ApolloCall<HeroTypeDependentAliasedField.Data> call = apolloClient.newCall(aliasedQuery);
    Response<HeroTypeDependentAliasedField.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:JEDI)");

    final Record hero = cacheStore.loadRecord(heroReference.key());
    assertThat(hero.field("primaryFunction")).isEqualTo("Astromech");
    assertThat(hero.field("__typename")).isEqualTo("Droid");
  }

  @Test
  public void testHeroTypeDependentAliasedFieldQueryHuman() throws IOException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedField aliasedQuery = new HeroTypeDependentAliasedField(
        HeroTypeDependentAliasedField.Variables.builder().episode(EMPIRE).build()
    );

    ApolloCall<HeroTypeDependentAliasedField.Data> call = apolloClient.newCall(aliasedQuery);
    Response<HeroTypeDependentAliasedField.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:EMPIRE)");

    final Record hero = cacheStore.loadRecord(heroReference.key());
    assertThat(hero.field("homePlanet")).isEqualTo("Tatooine");
    assertThat(hero.field("__typename")).isEqualTo("Human");
  }

  @Test
  public void testHeroParentTypeDependentAliasedFieldQueryHuman() throws IOException {
    MockResponse mockResponse = mockResponse("HeroTypeDependentAliasedFieldResponseHuman.json");
    server.enqueue(mockResponse);

    final HeroTypeDependentAliasedField aliasedQuery = new HeroTypeDependentAliasedField(
        HeroTypeDependentAliasedField.Variables.builder().episode(EMPIRE).build()
    );

    ApolloCall<HeroTypeDependentAliasedField.Data> call = apolloClient.newCall(aliasedQuery);
    Response<HeroTypeDependentAliasedField.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Record record = cacheStore.loadRecord(QUERY_ROOT_KEY);
    CacheReference heroReference = (CacheReference) record.field("hero(episode:EMPIRE)");

    final Record hero = cacheStore.loadRecord(heroReference.key());
    assertThat(hero.field("homePlanet")).isEqualTo("Tatooine");
    assertThat(hero.field("__typename")).isEqualTo("Human");
  }


//  TODO: Add these tests once https://github.com/apollographql/apollo-android/issues/263 is fixed
//  func testHeroParentTypeDependentFieldDroid() throws {
//    let query = HeroParentTypeDependentFieldQuery()
//
//    let response = GraphQLResponse(operation: query, body: [
//    "data": [
//    "hero": [
//    "name": "R2-D2",
//        "__typename": "Droid",
//        "friends": [
//    ["__typename": "Human", "name": "Luke Skywalker", "height": 1.72],
//    ]
//    ]
//    ]
//    ])
//
//    let (_, records) = try response.parseResult()
//
//    guard let luke = records?["hero.friends.0"] else { XCTFail(); return }
//    XCTAssertEqual(luke["height(unit:METER)"] as? Double, 1.72)
//  }
//
//  func testHeroParentTypeDependentFieldHuman() throws {
//    let query = HeroParentTypeDependentFieldQuery()
//
//    let response = GraphQLResponse(operation: query, body: [
//    "data": [
//    "hero": [
//    "name": "Luke Skywalker",
//        "__typename": "Human",
//        "friends": [
//    ["__typename": "Human", "name": "Han Solo", "height": 5.905512],
//    ]
//    ]
//    ]
//    ])
//
//    let (_, records) = try response.parseResult()
//
//    guard let han = records?["hero.friends.0"] else { XCTFail(); return }
//    XCTAssertEqual(han["height(unit:FOOT)"] as? Double, 5.905512)
//  }


}
