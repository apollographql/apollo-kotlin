package com.apollographql.apollo;

import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameWithEnumsQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameWithIdQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class OptimisticCacheTestCase {
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

  @Test public void simple() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"));
    HeroAndFriendsNamesQuery query = new HeroAndFriendsNamesQuery(Episode.JEDI);
    apolloClient.query(query).execute();

    UUID mutationId = UUID.randomUUID();
    HeroAndFriendsNamesQuery.Data data = new HeroAndFriendsNamesQuery.Data(new HeroAndFriendsNamesQuery.Hero(
        "Droid",
        "R222-D222",
        Arrays.asList(
            new HeroAndFriendsNamesQuery.Friend(
                "Human",
                "SuperMan"
            ),
            new HeroAndFriendsNamesQuery.Friend(
                "Human",
                "Batman"
            )
        )
    ));
    apolloClient.apolloStore().writeOptimisticUpdatesAndPublish(query, data, mutationId).execute();

    data = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data.hero().name()).isEqualTo("R222-D222");
    assertThat(data.hero().friends()).hasSize(2);
    assertThat(data.hero().friends().get(0).name()).isEqualTo("SuperMan");
    assertThat(data.hero().friends().get(1).name()).isEqualTo("Batman");

    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId).execute();

    data = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data.hero().name()).isEqualTo("R2-D2");
    assertThat(data.hero().friends()).hasSize(3);
    assertThat(data.hero().friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(data.hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(data.hero().friends().get(2).name()).isEqualTo("Leia Organa");
  }

  @Test public void two_optimistic_two_rollback() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDsQuery query1 = new HeroAndFriendsNamesWithIDsQuery(Episode.JEDI);
    apolloClient.query(query1).execute();

    UUID mutationId1 = UUID.randomUUID();
    HeroAndFriendsNamesWithIDsQuery.Data data1 = new HeroAndFriendsNamesWithIDsQuery.Data(
        new HeroAndFriendsNamesWithIDsQuery.Hero(
            "Droid",
            "2001",
            "R222-D222",
            Arrays.asList(
                new HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1000",
                    "SuperMan"
                ),
                new HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1003",
                    "Batman"
                )
            )
        )
    );
    apolloClient.apolloStore().writeOptimisticUpdatesAndPublish(query1, data1, mutationId1).execute();

    // check if query1 see optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R222-D222");
    assertThat(data1.hero().friends()).hasSize(2);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("SuperMan");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Batman");

    server.enqueue(mockResponse("HeroNameWithIdResponse.json"));
    HeroNameWithIdQuery query2 = new HeroNameWithIdQuery();
    apolloClient.query(query2).execute();

    UUID mutationId2 = UUID.randomUUID();
    HeroNameWithIdQuery.Data data2 = new HeroNameWithIdQuery.Data(new HeroNameWithIdQuery.Hero(
        "Human",
        "1000",
        "Beast"
    ));
    apolloClient.apolloStore().writeOptimisticUpdatesAndPublish(query2, data2, mutationId2).execute();

    // check if query1 see the latest optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R222-D222");
    assertThat(data1.hero().friends()).hasSize(2);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("Beast");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Batman");

    // check if query2 see the latest optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("Beast");

    // rollback query1 optimistic updates
    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId1).execute();

    // check if query1 see the latest optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R2-D2");
    assertThat(data1.hero().friends()).hasSize(3);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("Beast");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1002");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(data1.hero().friends().get(2).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(2).name()).isEqualTo("Leia Organa");

    // check if query2 see the latest optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("Beast");

    // rollback query2 optimistic updates
    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId2).execute();

    // check if query2 see the latest optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("SuperMan");
  }

  @Test public void full_persisted_partial_optimistic() throws Exception {
    server.enqueue(mockResponse("HeroNameWithEnumsResponse.json"));
    apolloClient.query(new HeroNameWithEnumsQuery()).execute();

    UUID mutationId = UUID.randomUUID();
    apolloClient.apolloStore().writeOptimisticUpdates(
        new HeroNameQuery(),
        new HeroNameQuery.Data(new HeroNameQuery.Hero("Droid", "R22-D22")),
        mutationId
    ).execute();

    HeroNameWithEnumsQuery.Data data = apolloClient.query(new HeroNameWithEnumsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data.hero().name()).isEqualTo("R22-D22");
    assertThat(data.hero().firstAppearsIn()).isEqualTo(Episode.EMPIRE);
    assertThat(data.hero().appearsIn()).isEqualTo(Arrays.asList(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI));

    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId).execute();

    data = apolloClient.query(new HeroNameWithEnumsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data.hero().name()).isEqualTo("R2-D2");
    assertThat(data.hero().firstAppearsIn()).isEqualTo(Episode.EMPIRE);
    assertThat(data.hero().appearsIn()).isEqualTo(Arrays.asList(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI));
  }
}
