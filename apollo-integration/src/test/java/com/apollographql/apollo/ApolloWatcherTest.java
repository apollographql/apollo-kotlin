package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloWatcherTest {
  private ApolloClient apolloClient;
  private MockWebServer server;
  private static final int TIME_OUT_SECONDS = 3;

  @Before public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  @Test
  public void testQueryWatcherUpdated_SameQuery_DifferentResults() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    // Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotUpdated_SameQuery_SameResults() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    // Wait 3 seconds to make sure no double callback.
    // Successful if timeout _is_ reached
    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherUpdated_DifferentQuery_DifferentResults() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    HeroAndFriendsNamesWithIDs friendsQuery = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).execute();

    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotUpdated_DifferentQueries() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    HeroAndFriendsNamesWithIDs friendsQuery = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    // Wait 3 seconds to make sure no double callback.
    // Successful if timeout _is_ reached
    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testRefetchCacheControl() throws IOException, InterruptedException, TimeoutException {
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);
    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();
    watcher.refetchCacheControl(CacheControl.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            new ApolloCall.Callback<EpisodeHeroName.Data>() {
              @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    //A different call gets updated information.
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));

    //To verify that the updated response comes from server use a different name change
    // -- this is for the refetch
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChangeTwo.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() throws IOException, InterruptedException,
      TimeoutException {
    final NamedCountDownLatch cacheWarmUpLatch = new NamedCountDownLatch("cacheWarmUpLatch", 1);
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.newCall(query).enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        cacheWarmUpLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Assert.fail(e.getMessage());
        cacheWarmUpLatch.countDown();
      }
    });
    cacheWarmUpLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    //Cache is now "warm" with response data

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query)
        .cacheControl(CacheControl.CACHE_ONLY)
        .watcher();
    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    watcher.cancel();
  }

  @Test
  public void testQueryWatcherNotCalled_WhenCanceled() throws IOException, TimeoutException, InterruptedException, ApolloException {

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    watcher.enqueueAndWatch(
        new ApolloCall.Callback<EpisodeHeroName.Data>() {
          @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    watcher.cancel();
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();

    //Wait for 3 seconds to check that callback is not called twice.
    //Test is successful if timeout is reached.
    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
