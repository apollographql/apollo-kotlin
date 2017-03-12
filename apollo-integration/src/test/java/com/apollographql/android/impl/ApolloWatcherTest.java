package com.apollographql.android.impl;

import android.support.annotation.NonNull;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.normalized.CacheControl;
import com.apollographql.android.cache.normalized.CacheKey;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.type.Episode;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloWatcherTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

  private ApolloClient apolloClient;
  private MockWebServer server;
  private InMemoryCacheStore cacheStore;

  @Before public void setUp() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    cacheStore = new InMemoryCacheStore();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(cacheStore, new CacheKeyResolver() {
          @Nonnull @Override public CacheKey resolve(@NonNull Map<String, Object> jsonObject) {
            String id = (String) jsonObject.get("id");
            if (id == null || id.isEmpty()) {
              return CacheKey.NO_KEY;
            }
            return CacheKey.from(id);
          }
        })
        .build();
  }

  @Test
  public void testQueryWatcherUpdated_SameQuery_DifferentResults() throws IOException, InterruptedException {
    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);

    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
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

          @Override public void onFailure(@Nonnull Exception e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }
        });

    firstResponseLatch.await();
    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.await();
  }

  @Test
  public void testQueryWatcherNotUpdated_SameQuery_SameResults() throws IOException, InterruptedException {
    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);

    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
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

          @Override public void onFailure(@Nonnull Exception e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }
        });

    firstResponseLatch.await();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);
    secondResponseLatch.await(3, TimeUnit.SECONDS); //Wait 3 seconds to make sure no double callback
  }

  @Test
  public void testQueryWatcherUpdated_DifferentQuery_DifferentResults() throws IOException, InterruptedException {
    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
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

          @Override public void onFailure(@Nonnull Exception e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }
        });

    firstResponseLatch.await();
    HeroAndFriendsNamesWithIDs friendsQuery = new HeroAndFriendsNamesWithIDs(
        HeroAndFriendsNamesWithIDs.Variables.builder().episode(Episode.NEWHOPE).build());

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.await();
  }

  @Test
  public void testQueryWatcherNotUpdated_DifferentQueries() throws IOException, InterruptedException {
    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
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

          @Override public void onFailure(@Nonnull Exception e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }
        });

    firstResponseLatch.await();
    HeroAndFriendsNamesWithIDs friendsQuery = new HeroAndFriendsNamesWithIDs(
        HeroAndFriendsNamesWithIDs.Variables.builder().episode(Episode.NEWHOPE).build());

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);
    secondResponseLatch.await(3, TimeUnit.SECONDS); //Wait 3 seconds to make sure no double callback
  }

  @Test
  public void rewatchCacheControl() throws IOException, InterruptedException {
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());

    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);
    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
    watcher.refetchCacheControl(CacheControl.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            new ApolloCall.Callback<EpisodeHeroName.Data>() {
              @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
                if (secondResponseLatch.getCount() == 2) {
                  assertThat(response.data().hero().name()).isEqualTo("R2-D2");
                } else if (secondResponseLatch.getCount() == 1) {
                  assertThat(response.data().hero().name()).isEqualTo("ArTwo");
                }
                firstResponseLatch.countDown();
                secondResponseLatch.countDown();
              }

              @Override public void onFailure(@Nonnull Exception e) {
                Assert.fail(e.getMessage());
                firstResponseLatch.countDown();
                secondResponseLatch.countDown();
              }
            });

    firstResponseLatch.await();
    //Another newer call gets updated information -- need to queue up two network responses
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));

    //To verify that the updated response comes from server use a different name change
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChangeTwo.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);
    secondResponseLatch.await();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
