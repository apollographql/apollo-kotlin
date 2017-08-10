package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameWithEnumsQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameWithIdQuery;
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisodeQuery;
import com.apollographql.apollo.integration.normalizer.UpdateReviewMutation;
import com.apollographql.apollo.integration.normalizer.type.ColorInput;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.integration.normalizer.type.ReviewInput;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

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
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
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

  @Test public void mutation_and_query_watcher() throws Exception {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"));
    final NamedCountDownLatch watcherFirstCallLatch = new NamedCountDownLatch("WatcherFirstCall", 1);
    final List<ReviewsByEpisodeQuery.Data> watcherData = new ArrayList<>();
    apolloClient.query(new ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST)
        .watcher().refetchResponseFetcher(ApolloResponseFetchers.CACHE_FIRST)
        .enqueueAndWatch(new ApolloCall.Callback<ReviewsByEpisodeQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<ReviewsByEpisodeQuery.Data> response) {
            watcherData.add(response.data());
            watcherFirstCallLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            watcherFirstCallLatch.countDown();
          }
        });
    watcherFirstCallLatch.awaitOrThrowWithTimeout(2, TimeUnit.SECONDS);

    server.enqueue(mockResponse("UpdateReviewResponse.json").setBodyDelay(2, TimeUnit.SECONDS));
    UpdateReviewMutation updateReviewMutation = new UpdateReviewMutation(
        "empireReview2",
        ReviewInput.builder()
            .commentary("Great")
            .stars(5)
            .favoriteColor(ColorInput.builder().build())
            .build()
    );
    final NamedCountDownLatch mutationCallLatch = new NamedCountDownLatch("MutationCall", 1);
    apolloClient.mutate(updateReviewMutation, new UpdateReviewMutation.Data(new UpdateReviewMutation.UpdateReview(
        "Review", "empireReview2", 5, "Great"))).enqueue(
        new ApolloCall.Callback<UpdateReviewMutation.Data>() {
          @Override public void onResponse(@Nonnull Response<UpdateReviewMutation.Data> response) {
            mutationCallLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            mutationCallLatch.countDown();
          }
        }
    );
    mutationCallLatch.await(3, TimeUnit.SECONDS);

    // sleep a while to wait if watcher gets another notification
    Thread.sleep(TimeUnit.SECONDS.toMillis(2));

    assertThat(watcherData).hasSize(3);

    // before mutation and optimistic updates
    assertThat(watcherData.get(0).reviews()).hasSize(3);
    assertThat(watcherData.get(0).reviews().get(0).id()).isEqualTo("empireReview1");
    assertThat(watcherData.get(0).reviews().get(0).stars()).isEqualTo(1);
    assertThat(watcherData.get(0).reviews().get(0).commentary()).isEqualTo("Boring");
    assertThat(watcherData.get(0).reviews().get(1).id()).isEqualTo("empireReview2");
    assertThat(watcherData.get(0).reviews().get(1).stars()).isEqualTo(2);
    assertThat(watcherData.get(0).reviews().get(1).commentary()).isEqualTo("So-so");
    assertThat(watcherData.get(0).reviews().get(2).id()).isEqualTo("empireReview3");
    assertThat(watcherData.get(0).reviews().get(2).stars()).isEqualTo(5);
    assertThat(watcherData.get(0).reviews().get(2).commentary()).isEqualTo("Amazing");

    // optimistic updates
    assertThat(watcherData.get(1).reviews()).hasSize(3);
    assertThat(watcherData.get(1).reviews().get(0).id()).isEqualTo("empireReview1");
    assertThat(watcherData.get(1).reviews().get(0).stars()).isEqualTo(1);
    assertThat(watcherData.get(1).reviews().get(0).commentary()).isEqualTo("Boring");
    assertThat(watcherData.get(1).reviews().get(1).id()).isEqualTo("empireReview2");
    assertThat(watcherData.get(1).reviews().get(1).stars()).isEqualTo(5);
    assertThat(watcherData.get(1).reviews().get(1).commentary()).isEqualTo("Great");
    assertThat(watcherData.get(1).reviews().get(2).id()).isEqualTo("empireReview3");
    assertThat(watcherData.get(1).reviews().get(2).stars()).isEqualTo(5);
    assertThat(watcherData.get(1).reviews().get(2).commentary()).isEqualTo("Amazing");

    // after mutation with rolled back optimistic updates
    assertThat(watcherData.get(2).reviews()).hasSize(3);
    assertThat(watcherData.get(2).reviews().get(0).id()).isEqualTo("empireReview1");
    assertThat(watcherData.get(2).reviews().get(0).stars()).isEqualTo(1);
    assertThat(watcherData.get(2).reviews().get(0).commentary()).isEqualTo("Boring");
    assertThat(watcherData.get(2).reviews().get(1).id()).isEqualTo("empireReview2");
    assertThat(watcherData.get(2).reviews().get(1).stars()).isEqualTo(4);
    assertThat(watcherData.get(2).reviews().get(1).commentary()).isEqualTo("Not Bad");
    assertThat(watcherData.get(2).reviews().get(2).id()).isEqualTo("empireReview3");
    assertThat(watcherData.get(2).reviews().get(2).stars()).isEqualTo(5);
    assertThat(watcherData.get(2).reviews().get(2).commentary()).isEqualTo("Amazing");
  }

  @Test public void two_optimistic_reverse_rollback_order() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    HeroAndFriendsNamesWithIDsQuery query1 = new HeroAndFriendsNamesWithIDsQuery(Episode.JEDI);
    apolloClient.query(query1).execute();

    server.enqueue(mockResponse("HeroNameWithIdResponse.json"));
    HeroNameWithIdQuery query2 = new HeroNameWithIdQuery();
    apolloClient.query(query2).execute();

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
                    "Robocop"
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

    UUID mutationId2 = UUID.randomUUID();
    HeroNameWithIdQuery.Data data2 = new HeroNameWithIdQuery.Data(new HeroNameWithIdQuery.Hero(
        "Human",
        "1000",
        "Spiderman"
    ));
    apolloClient.apolloStore().writeOptimisticUpdatesAndPublish(query2, data2, mutationId2).execute();

    // check if query1 see optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R222-D222");
    assertThat(data1.hero().friends()).hasSize(2);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("Spiderman");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Batman");

    // check if query2 see the latest optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("Spiderman");

    // rollback query2 optimistic updates
    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId2).execute();

    // check if query1 see the latest optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R222-D222");
    assertThat(data1.hero().friends()).hasSize(2);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("Robocop");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Batman");

    // check if query2 see the latest optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("Robocop");

    // rollback query1 optimistic updates
    apolloClient.apolloStore().rollbackOptimisticUpdates(mutationId1).execute();

    // check if query1 see the latest non-optimistic updates
    data1 = apolloClient.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data1.hero().id()).isEqualTo("2001");
    assertThat(data1.hero().name()).isEqualTo("R2-D2");
    assertThat(data1.hero().friends()).hasSize(3);
    assertThat(data1.hero().friends().get(0).id()).isEqualTo("1000");
    assertThat(data1.hero().friends().get(0).name()).isEqualTo("SuperMan");
    assertThat(data1.hero().friends().get(1).id()).isEqualTo("1002");
    assertThat(data1.hero().friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(data1.hero().friends().get(2).id()).isEqualTo("1003");
    assertThat(data1.hero().friends().get(2).name()).isEqualTo("Leia Organa");

    // check if query2 see the latest non-optimistic updates
    data2 = apolloClient.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data();
    assertThat(data2.hero().id()).isEqualTo("1000");
    assertThat(data2.hero().name()).isEqualTo("SuperMan");
  }
}
