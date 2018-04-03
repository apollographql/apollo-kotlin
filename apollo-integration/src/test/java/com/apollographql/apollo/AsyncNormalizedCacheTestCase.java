package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AsyncNormalizedCacheTestCase {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .build();
  }

  private MockResponse mockResponse(String fileName) throws IOException, ApolloException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  @Test public void testAsync() throws IOException, InterruptedException, ApolloException {
    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();

    for (int i = 0; i < 500; i++) {
      server.enqueue(mockResponse("HeroNameResponse.json"));
    }

    List<Observable<Response<EpisodeHeroNameQuery.Data>>> calls = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      ApolloQueryCall<EpisodeHeroNameQuery.Data> queryCall = apolloClient
          .query(query)
          .responseFetcher(i % 2 == 0 ? ApolloResponseFetchers.NETWORK_FIRST : ApolloResponseFetchers.CACHE_ONLY);
      calls.add(Rx2Apollo.from(queryCall));
    }
    TestObserver<Response<EpisodeHeroNameQuery.Data>> observer = new TestObserver<>();
    Observable.merge(calls).subscribe(observer);
    observer.awaitTerminalEvent();
    observer.assertNoErrors();
    observer.assertValueCount(1000);
    observer.assertNever(new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
      @Override public boolean test(Response<EpisodeHeroNameQuery.Data> dataResponse) throws Exception {
        return dataResponse.hasErrors();
      }
    });
  }
}
