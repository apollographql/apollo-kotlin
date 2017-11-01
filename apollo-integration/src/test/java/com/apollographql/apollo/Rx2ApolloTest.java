package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.TestObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.TIME_OUT_SECONDS;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.NEWHOPE;
import static com.google.common.truth.Truth.assertThat;

public class Rx2ApolloTest {
  private ApolloClient apolloClient;
  private MockWebServer server;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";

  @Before public void setup() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @After
  public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignore) {
      //ignore
    }
  }

  @Test
  public void callProducesValue() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete()
        .assertValue(new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            return true;
          }
        });
  }

  @Test
  public void callIsCanceledWhenDisposed() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<Response<EpisodeHeroNameQuery.Data>> testObserver = new TestObserver<>();
    Disposable disposable = Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .delay(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .subscribeWith(testObserver);

    disposable.dispose();

    testObserver.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testObserver.assertNotComplete();
    assertThat(testObserver.isDisposed()).isTrue();
  }

  @Test
  public void prefetchCompletes() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
  }

  @Test
  public void prefetchIsCanceledWhenDisposed() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroNameQuery.Data> testObserver = new TestObserver<>();
    Disposable disposable = Rx2Apollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .delay(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .subscribeWith(testObserver);

    disposable.dispose();

    testObserver.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    testObserver.assertNotComplete();
    assertThat(testObserver.isDisposed()).isTrue();
  }

  @Test
  public void queryWatcherUpdatedSameQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .doOnNext(new Consumer<EpisodeHeroNameQuery.Data>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void accept(EpisodeHeroNameQuery.Data data) throws Exception {
            if (executed.compareAndSet(false, true)) {
              server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)))
                  .responseFetcher(NETWORK_ONLY)
                  .enqueue(null);
            }
          }
        })
        .take(2)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertValueCount(2)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        })
        .assertValueAt(1, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("Artoo");
            return true;
          }
        });
  }

  @Test
  public void queryWatcherNotUpdatedSameQuerySameResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .doOnNext(new Consumer<EpisodeHeroNameQuery.Data>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void accept(EpisodeHeroNameQuery.Data data) throws Exception {
            if (executed.compareAndSet(false, true)) {
              server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
                  .enqueue(null);
            }
          }
        })
        .take(2)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        });
  }

  @Test
  public void queryWatcherUpdatedDifferentQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .doOnNext(new Consumer<EpisodeHeroNameQuery.Data>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void accept(EpisodeHeroNameQuery.Data data) throws Exception {
            if (executed.compareAndSet(false, true)) {
              server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
              apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(NEWHOPE))).enqueue(null);
            }
          }
        })
        .take(2)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertValueCount(2)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        })
        .assertValueAt(1, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("Artoo");
            return true;
          }
        });
  }

  @Test
  public void queryWatcherNotCalledWhenCanceled() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroNameQuery.Data> testObserver = new TestObserver<>();
    Disposable disposable = Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .doOnNext(new Consumer<EpisodeHeroNameQuery.Data>() {
          AtomicBoolean executed = new AtomicBoolean();

          @Override public void accept(EpisodeHeroNameQuery.Data data) throws Exception {
            if (executed.compareAndSet(false, true)) {
              server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE).setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));
              apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
                  .enqueue(null);
            }
          }
        })
        .subscribeWith(testObserver);

    try {
      Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_OUT_SECONDS));
    } catch (Exception ignore) {
    }
    disposable.dispose();

    testObserver
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        });
  }
}
