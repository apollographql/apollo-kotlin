package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.android.rx2.Rx2Apollo;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class Rx2ApolloTest {

  private ApolloClient apolloClient;
  private MockWebServer mockWebServer;
  private InMemoryCacheStore inMemoryCacheStore;

  private static final long TIME_OUT_SECONDS = 3;

  @Before public void setup() {
    mockWebServer = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    inMemoryCacheStore = new InMemoryCacheStore();

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

  @Test
  public void testRx2CallProducesValue() throws IOException, InterruptedException {

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    EpisodeHeroName.Data data = Rx2Apollo.from(apolloClient.newCall(query))
        .test()
        .await()
        .assertNoErrors()
        .assertComplete()
        .values()
        .get(0);

    assertThat(data.hero().name()).isEqualTo("R2-D2");
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
