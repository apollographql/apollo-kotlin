package com.apollographql.android.impl;

import android.support.annotation.NonNull;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.cache.normalized.CacheControl;
import com.apollographql.android.cache.normalized.CacheKey;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.android.rx.RxApollo;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import rx.Observer;
import rx.Subscription;
import rx.observers.TestSubscriber;

import static com.google.common.truth.Truth.assertThat;

public class RxApolloTest {
  private ApolloClient apolloClient;
  private MockWebServer server;
  private InMemoryCacheStore cacheStore;
  private static final long TIME_OUT_SECONDS = 3;

  @Before public void setUp() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    cacheStore = new InMemoryCacheStore();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(cacheStore, new CacheKeyResolver<Map<String, Object>>() {
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


  @Test public void testRxCallProducesValue() throws IOException {
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    EpisodeHeroName.Data data = RxApollo
        .from(apolloClient.newCall(query))
        .test()
        .awaitTerminalEvent()
        .assertNoErrors()
        .assertCompleted()
        .getOnNextEvents()
        .get(0);
    assertThat(data.hero().name()).isEqualTo("R2-D2");
  }

  @Test public void textRxCallIsCancelledWhenUnsubscribed() throws IOException {
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloCall<EpisodeHeroName.Data> call = apolloClient.newCall(query);
    TestSubscriber<EpisodeHeroName.Data> subscriber = new TestSubscriber<>();
    Subscription subscription = RxApollo
        .from(call)
        .delay(5, TimeUnit.SECONDS)
        .subscribe(subscriber);
    subscription.unsubscribe();
    subscriber
        .assertUnsubscribed();
    subscriber.assertNoValues();

  }

  @Test
  public void testRxQueryWatcherUpdated_SameQuery_DifferentResults() throws IOException, InterruptedException, TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<EpisodeHeroName.Data>() {
      @Override public void onCompleted() {
      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(EpisodeHeroName.Data data) {
        if (secondResponseLatch.getCount() == 2) {
          assertThat(data.hero().name()).isEqualTo("R2-D2");
        } else if (secondResponseLatch.getCount() == 1) {
          assertThat(data.hero().name()).isEqualTo("Artoo");
        }
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void testRxQueryWatcherNotUpdated_SameQuery_SameResults() throws IOException, InterruptedException, TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<EpisodeHeroName.Data>() {
      @Override public void onCompleted() {
      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(EpisodeHeroName.Data data) {
        assertThat(data.hero().name()).isEqualTo("R2-D2");
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
        if (secondResponseLatch.getCount() == 0) {
          Assert.fail("Received two callbacks, although data should not have changed.");
        }
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);;
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    // Wait 3 seconds to make sure no double callback.
    // Successful if timeout _is_ reached
    secondResponseLatch.await(3, TimeUnit.SECONDS);
  }

  @Test
  public void testQueryRxWatcherUpdated_DifferentQuery_DifferentResults() throws IOException, InterruptedException, TimeoutException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    RxApollo.from(watcher).subscribe(new Observer<EpisodeHeroName.Data>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        Assert.fail(e.getMessage());
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }

      @Override public void onNext(EpisodeHeroName.Data data) {
        if (secondResponseLatch.getCount() == 2) {
          assertThat(data.hero().name()).isEqualTo("R2-D2");
        } else if (secondResponseLatch.getCount() == 1) {
          assertThat(data.hero().name()).isEqualTo("Artoo");
        }
        firstResponseLatch.countDown();
        secondResponseLatch.countDown();
      }
    });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);;
    HeroAndFriendsNamesWithIDs friendsQuery = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);;
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
