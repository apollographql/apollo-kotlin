package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.immediateExecutor;
import static com.apollographql.apollo.Utils.immediateExecutorService;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.NEWHOPE;
import static com.google.common.truth.Truth.assertThat;

public class Rx2ApolloTest {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";

  @Before public void setup() {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(immediateExecutor())
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  @Test
  public void callProducesValue() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
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
        .subscribeWith(testObserver);

    disposable.dispose();

    testObserver.assertComplete();
    assertThat(testObserver.isDisposed()).isTrue();
  }

  @Test
  public void prefetchCompletes() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    Rx2Apollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
        .assertNoErrors()
        .assertComplete();
  }

  @Test
  public void prefetchIsCanceledWhenDisposed() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroNameQuery.Data> testObserver = new TestObserver<>();
    Disposable disposable = Rx2Apollo
        .from(apolloClient.prefetch(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .observeOn(new TestScheduler())
        .subscribeWith(testObserver);

    disposable.dispose();

    testObserver.assertNotComplete();
    assertThat(testObserver.isDisposed()).isTrue();
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void queryWatcherUpdatedSameQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    TestObserver<EpisodeHeroNameQuery.Data> observer = new TestObserver<>();
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .subscribeWith(observer);

    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)))
        .responseFetcher(NETWORK_ONLY)
        .enqueue(null);

    observer.assertValueCount(2)
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
  @SuppressWarnings("CheckReturnValue")
  public void queryWatcherNotUpdatedSameQuerySameResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    TestObserver<EpisodeHeroNameQuery.Data> observer = new TestObserver<>();
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .subscribeWith(observer);

    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
        .enqueue(null);

    observer
        .assertValueCount(1)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        });
  }

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void queryWatcherUpdatedDifferentQueryDifferentResults() throws Exception {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroNameQuery.Data> observer = new TestObserver<>();
    Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .subscribeWith(observer);

    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.query(new HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(NEWHOPE))).enqueue(null);

    observer
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
    TestScheduler scheduler = new TestScheduler();
    Disposable disposable = Rx2Apollo
        .from(apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map(new Function<Response<EpisodeHeroNameQuery.Data>, EpisodeHeroNameQuery.Data>() {
          @Override
          public EpisodeHeroNameQuery.Data apply(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            return response.data();
          }
        })
        .observeOn(scheduler)
        .subscribeWith(testObserver);

    scheduler.triggerActions();
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
    apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
        .enqueue(null);
    disposable.dispose();
    scheduler.triggerActions();

    testObserver
        .assertValueCount(1)
        .assertValueAt(0, new Predicate<EpisodeHeroNameQuery.Data>() {
          @Override public boolean test(EpisodeHeroNameQuery.Data data) throws Exception {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            return true;
          }
        });
  }
}
