package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.HeroAndFriendsNamesWithIDs;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.android.rx2.Rx2Apollo;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.TestObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class Rx2ApolloTest {

  private ApolloClient apolloClient;
  private MockWebServer mockWebServer;

  private static final int RX_DELAY_SECONDS = 2;
  private static final long TIME_OUT_SECONDS = 3;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json";


  @Before public void setup() {
    mockWebServer = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    InMemoryNormalizedCache inMemoryCacheStore = new InMemoryNormalizedCache();

    apolloClient = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(inMemoryCacheStore, new CacheKeyResolver<Map<String, Object>>() {
          @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> objectSource) {
            String id = (String) objectSource.get("id");
            if (id == null || id.isEmpty()) {
              return CacheKey.NO_KEY;
            }
            return CacheKey.from(id);
          }
        })
        .build();
  }

  @After
  public void tearDown() {
    try {
      mockWebServer.shutdown();
    } catch (IOException e) {

    }
  }

  @Test
  public void testRx2CallProducesValue() throws IOException, InterruptedException {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    EpisodeHeroName.Data data = Rx2Apollo
        .from(apolloClient.newCall(query))
        .test()
        .await()
        .assertNoErrors()
        .assertComplete()
        .values()
        .get(0);

    assertThat(data.hero().name()).isEqualTo("R2-D2");
  }

  @Test
  public void testRx2CallIsCanceledWhenDisposed() throws IOException {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroName.Data> testObserver = new TestObserver<>();

    Disposable disposable = Rx2Apollo
        .from(apolloClient.newCall(query))
        .delay(5, TimeUnit.SECONDS)
        .subscribeWith(testObserver);

    disposable.dispose();

    assertThat(testObserver.isDisposed()).isTrue();
    testObserver.assertNoValues();
  }

  @Test
  public void testRx2PrefetchCompletes() throws IOException, InterruptedException {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    Rx2Apollo
        .from(apolloClient.prefetch(query))
        .test()
        .await()
        .assertNoErrors()
        .assertComplete();
  }

  @Test
  public void testRx2PrefetchIsCanceledWhenDisposed() throws IOException {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    TestObserver<EpisodeHeroName.Data> testObserver = new TestObserver<>();

    Disposable disposable = Rx2Apollo
        .from(apolloClient.prefetch(query))
        .delay(RX_DELAY_SECONDS, TimeUnit.SECONDS)
        .subscribeWith(testObserver);

    disposable.dispose();

    assertThat(testObserver.isDisposed()).isTrue();
    testObserver.assertNotComplete();
  }

  @Test
  public void testRx2QueryWatcherUpdated_SameQuery_DifferentResults()
      throws IOException, TimeoutException, InterruptedException, ApolloException {

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    Rx2Apollo
        .from(watcher)
        .subscribe(new DisposableObserver<EpisodeHeroName.Data>() {
          @Override public void onNext(EpisodeHeroName.Data data) {
            if (secondResponseLatch.getCount() == 2) {
              assertThat(data.hero().name()).isEqualTo("R2-D2");
            } else if (secondResponseLatch.getCount() == 1) {
              assertThat(data.hero().name()).isEqualTo("Artoo");
            }
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onError(Throwable e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onComplete() {

          }
        });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    //Another newer call gets updated information
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void testRx2QueryWatcherNotUpdated_SameQuery_SameResults()
      throws IOException, TimeoutException, InterruptedException, ApolloException {

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResultLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResultLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    Rx2Apollo
        .from(watcher)
        .subscribe(new DisposableObserver<EpisodeHeroName.Data>() {
          @Override public void onNext(EpisodeHeroName.Data data) {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
            if (secondResponseLatch.getCount() == 0) {
              Assert.fail("received two callbacks, although data shouldn't change");
            }
          }

          @Override public void onError(Throwable e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onComplete() {

          }
        });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null);

    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void testRx2QueryWatcherUpdated_DifferentQuery_DifferentResults() throws IOException, InterruptedException,
      TimeoutException, ApolloException {
    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    Rx2Apollo
        .from(watcher)
        .subscribe(new DisposableObserver<EpisodeHeroName.Data>() {

          @Override public void onNext(EpisodeHeroName.Data data) {
            if (secondResponseLatch.getCount() == 2) {
              assertThat(data.hero().name()).isEqualTo("R2-D2");
            } else if (secondResponseLatch.getCount() == 1) {
              assertThat(data.hero().name()).isEqualTo("Artoo");
            }
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onComplete() {

          }

          @Override public void onError(Throwable e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }
        });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    HeroAndFriendsNamesWithIDs friendsQuery = HeroAndFriendsNamesWithIDs.builder().episode(Episode.NEWHOPE).build();

    mockWebServer.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"));
    apolloClient.newCall(friendsQuery).cacheControl(CacheControl.NETWORK_ONLY).execute();
    secondResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

  }

  @Test
  public void testRx2QueryWatcherNotCalled_WhenCanceled()
      throws IOException, TimeoutException, InterruptedException, ApolloException {

    final NamedCountDownLatch firstResponseLatch = new NamedCountDownLatch("firstResponseLatch", 1);
    final NamedCountDownLatch secondResponseLatch = new NamedCountDownLatch("secondResponseLatch", 2);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).watcher();

    Disposable disposable = Rx2Apollo
        .from(watcher)
        .subscribeWith(new DisposableObserver<EpisodeHeroName.Data>() {
          @Override public void onNext(EpisodeHeroName.Data data) {
            assertThat(data.hero().name()).isEqualTo("R2-D2");
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
            if (secondResponseLatch.getCount() == 0) {
              Assert.fail("Received two onNext, although RxQueryWatcher has already been canceled");
            }
          }

          @Override public void onError(Throwable e) {
            Assert.fail(e.getMessage());
            firstResponseLatch.countDown();
            secondResponseLatch.countDown();
          }

          @Override public void onComplete() {

          }
        });

    firstResponseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE));
    disposable.dispose();
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).execute();

    secondResponseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
