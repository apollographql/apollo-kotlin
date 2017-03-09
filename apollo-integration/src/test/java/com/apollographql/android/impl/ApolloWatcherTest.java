package com.apollographql.android.impl;

import android.support.annotation.NonNull;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.normalized.CacheControl;
import com.apollographql.android.cache.normalized.CacheKey;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

  private final String QUERY_ROOT_KEY = "QUERY_ROOT";

  @Before public void setUp() {
    server = new MockWebServer();
    CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(String value) {
        try {
          return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public String encode(Date value) {
        return DATE_FORMAT.format(value);
      }
    };

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
  public void testQueryWatcher() throws IOException, InterruptedException {
    server.enqueue(mockResponse("HeroNameResponse.json"));
    EpisodeHeroName query = new EpisodeHeroName(EpisodeHeroName.Variables.builder().episode(Episode.EMPIRE).build());

    final CountDownLatch firstResponseLatch = new CountDownLatch(1);
    final CountDownLatch secondResponseLatch = new CountDownLatch(2);
    ApolloWatcher<EpisodeHeroName.Data> watcher = apolloClient.newCall(query).toWatcher();
    final ApolloWatcher.WatcherSubscription watcherSubscription = watcher.enqueueAndWatch(new ApolloCall.Callback<EpisodeHeroName.Data>() {
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

    firstResponseLatch.await(); // add timeout ?
    server.enqueue(mockResponse("HeroNameResponseNameChange.json"));
    apolloClient.newCall(query).cacheControl(CacheControl.NETWORK_ONLY).enqueue(null); //Another newer call gets
    secondResponseLatch.await(); // add tiemout?
    watcherSubscription.unsubscribe();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
