package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableStore;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.functions.Predicate;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.TIME_OUT_SECONDS;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.Utils.immediateExecutor;
import static com.apollographql.apollo.Utils.immediateExecutorService;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;

public class ApolloWatcherTest {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() throws IOException {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(Utils.immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(immediateExecutor())
        .okHttpClient(okHttpClient)
        .logger(new Logger() {
          @Override
          public void log(int priority, @Nonnull String message, @Nonnull Optional<Throwable> t, @Nonnull Object... args) {
            String throwableTrace = "";
            if (t.isPresent()) {
              throwableTrace = t.get().getMessage();
            }
            System.out.println(String.format(message, args) + " " + throwableTrace);
          }
        })
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @Test
  public void testQueryWatcherUpdated_SameQuery_DifferentResults() throws Exception {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            if (secondResponseLatch.getCount() == 2) {
              assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            } else if (secondResponseLatch.getCount() == 1) {
              assertThat(response.data().hero().name()).isEqualTo("Artoo");
            }
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    firstResponseLatch.await();

    // Another newer call gets updated information
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient.query(query).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    secondResponseLatch.await();
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherUpdated_Store_write() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final List<String> responses = new ArrayList<>();
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
              responses.add(response.data().hero().name());
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    assertThat(responses.get(0)).isEqualTo("R2-D2");

    // Someone writes to the store directly
    Set<String> changedKeys = apolloClient.apolloStore().writeTransaction(new Transaction<WriteableStore, Set<String>>() {
      @Nullable @Override public Set<String> execute(WriteableStore cache) {
        Record record = Record.builder("2001")
            .addField("name", "Artoo")
            .build();
        return cache.merge(Collections.singletonList(record), CacheHeaders.NONE);
      }
    });
    apolloClient.apolloStore().publish(changedKeys);

    assertThat(responses.get(1)).isEqualTo("Artoo");

    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotUpdated_SameQuery_SameResults() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
            if (secondResponseLatch.getCount() == 0) {
              Assert.fail("Received two callbacks, although data should not have changed.");
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).enqueue(null);

    watcher.cancel();
  }

  @Test
  public void testQueryWatcherUpdated_DifferentQuery_DifferentResults() throws Exception {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            if (secondResponseLatch.getCount() == 2) {
              assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            } else if (secondResponseLatch.getCount() == 1) {
              assertThat(response.data().hero().name()).isEqualTo("Artoo");
            }
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    firstResponseLatch.await();
    HeroAndFriendsNamesWithIDsQuery friendsQuery = HeroAndFriendsNamesWithIDsQuery.builder()
        .episode(Episode.NEWHOPE)
        .build();

    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsNameChange.json",
        apolloClient.query(friendsQuery).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data>>() {
          @Override public boolean test(Response<HeroAndFriendsNamesWithIDsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    secondResponseLatch.await();
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotUpdated_DifferentQueries() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
            if (secondResponseLatch.getCount() == 0) {
              Assert.fail("Received two callbacks, although data should not have changed.");
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    HeroAndFriendsNamesWithIDsQuery friendsQuery = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    apolloClient.query(friendsQuery).responseFetcher(NETWORK_ONLY).enqueue(null);

    watcher.cancel();
  }

  @Test
  public void testRefetchCacheControl() throws IOException, InterruptedException, TimeoutException {
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);
    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();
    watcher.refetchResponseFetcher(NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
              @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
                if (secondResponseLatch.getCount() == 2) {
                  assertThat(response.data().hero().name()).isEqualTo("R2-D2");
                } else if (secondResponseLatch.getCount() == 1) {
                  assertThat(response.data().hero().name()).isEqualTo("ArTwo");
                } else {
                  Assert.fail("Unknown hero name: " + response.data().hero().name());
                }
                firstResponseLatch.countDown();
                secondResponseLatch.countDown();
              }

              @Override public void onFailure(@Nonnull ApolloException e) {
                Assert.fail(e.getCause().getMessage());
              }
            });

    //A different call gets updated information.
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));

    //To verify that the updated response comes from server use a different name change
    // -- this is for the refetch
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChangeTwo.json"));
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).enqueue(null);

    watcher.cancel();
  }

  @Test
  public void testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch cacheWarmUpLatch = new NamedCountDownLatch("cacheWarmUpLatch", 1);
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.query(query).enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        cacheWarmUpLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Assert.fail(e.getMessage());
        cacheWarmUpLatch.countDown();
      }
    });
    cacheWarmUpLatch.await();

    //Cache is now "warm" with response data

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query)
        .responseFetcher(CACHE_ONLY).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            if (secondResponseLatch.getCount() == 2) {
              assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            } else if (secondResponseLatch.getCount() == 1) {
              assertThat(response.data().hero().name()).isEqualTo("Artoo");
            }
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });

    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.query(query).responseFetcher(NETWORK_ONLY).enqueue(null);

    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotCalled_WhenCanceled() throws Exception {

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloQueryWatcher<EpisodeHeroNameQuery.Data> watcher = apolloClient.query(query).watcher();

    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
            if (secondResponseLatch.getCount() == 0) {
              Assert.fail("Received two callbacks, although query watcher has already been canceled");
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Assert.fail(e.getMessage());
          }
        });


    watcher.cancel();
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient.query(query).responseFetcher(NETWORK_ONLY),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

  }
}
