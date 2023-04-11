package test;

import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CompiledField;
import com.apollographql.apollo3.api.Executable;
import com.apollographql.apollo3.api.ScalarAdapter;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;
import com.apollographql.apollo3.cache.http.HttpCache;
import com.apollographql.apollo3.cache.http.HttpFetchPolicy;
import com.apollographql.apollo3.cache.normalized.NormalizedCache;
import com.apollographql.apollo3.cache.normalized.api.CacheKey;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.network.http.ApolloClientAwarenessInterceptor;
import com.apollographql.apollo3.network.http.BatchingHttpInterceptor;
import com.apollographql.apollo3.network.http.HttpNetworkTransport;
import com.apollographql.apollo3.rx2.Rx2Apollo;
import com.google.common.truth.Truth;
import io.reactivex.disposables.Disposable;
import javatest.AnimalCreatedSubscription;
import javatest.CreateCatMutation;
import javatest.GetRandomQuery;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ClientTest {
  MockServer mockServer;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = new MockServer();

    /**
     * Because url doesn't suspend on the JVM, we can just use the return value
     */
    String url = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    apolloClient = new ApolloClient.Builder().serverUrl(url).build();
  }

  @Test
  public void simple() {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    ApolloResponse<GetRandomQuery.Data> queryResponse = Rx2Apollo.single(
        apolloClient.query(GetRandomQuery.builder().build())
    ).blockingGet();
    Truth.assertThat(queryResponse.dataOrThrow().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}").build());
    ApolloResponse<CreateCatMutation.Data> mutationResponse = Rx2Apollo.single(
        apolloClient.mutation(CreateCatMutation.builder().build())
    ).blockingGet();
    Truth.assertThat(mutationResponse.dataOrThrow().createAnimal.catFragment.species).isEqualTo("cat");

    Disposable disposable = Rx2Apollo.flowable(
        apolloClient.subscription(AnimalCreatedSubscription.builder().build())
    ).subscribe(result -> {
      String species = result.dataOrThrow().animalCreated.catFragment.species;
    });
  }

  private void autoPersistedQueries() {
    apolloClient = new ApolloClient.Builder()
        .serverUrl("https://localhost")
        .autoPersistedQueries()
        .build();
  }

  private void queryBatching() {
    ApolloClient.Builder apolloClientBuilder = new ApolloClient.Builder()
        .serverUrl("https://localhost")
        .httpBatching();
    BatchingHttpInterceptor.configureApolloClientBuilder(apolloClientBuilder, false);
    apolloClient = apolloClientBuilder.build();

    ApolloCall<GetRandomQuery.Data> call = apolloClient.query(GetRandomQuery.builder().build());
    BatchingHttpInterceptor.configureApolloCall(call, true);
    ApolloResponse<GetRandomQuery.Data> result = Rx2Apollo.single(call).blockingGet();
  }

  private void httpCache() {
    ApolloClient.Builder apolloClientBuilder = new ApolloClient.Builder().serverUrl("https://localhost");
    File cacheDir = new File("/tmp/apollo-cache");
    long cacheSize = 10_000_000;
    HttpCache.configureApolloClientBuilder(apolloClientBuilder, cacheDir, cacheSize);
    apolloClient = apolloClientBuilder.build();

    ApolloCall<GetRandomQuery.Data> call = apolloClient.query(new GetRandomQuery());
    HttpCache.httpFetchPolicy(call, HttpFetchPolicy.NetworkOnly);
    ApolloResponse<GetRandomQuery.Data> result = Rx2Apollo.single(call).blockingGet();
  }

  private void normalizedCache() {
    ApolloClient.Builder apolloClientBuilder = new ApolloClient.Builder().serverUrl("https://localhost");

    NormalizedCacheFactory cacheFactory = new MemoryCacheFactory().chain(
        new SqlNormalizedCacheFactory("jdbc:sqlite:apollo.db")
    );

    // Using default cacheKeyGenerator/cacheResolver
    NormalizedCache.configureApolloClientBuilder(apolloClientBuilder, cacheFactory);

    // Using custom cacheKeyGenerator/cacheResolver

    CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator() {
      @Override public CacheKey cacheKeyForObject(@NotNull Map<String, ?> obj, @NotNull CacheKeyGeneratorContext context) {
        return new CacheKey(obj.get("id").toString());
      }
    };

    CacheKeyResolver cacheKeyResolver = new CacheKeyResolver() {
      @Override public CacheKey cacheKeyForField(@NotNull CompiledField field, @NotNull Executable.Variables variables) {
        String typename = field.getType().rawType().getName();
        Object id = field.resolveArgument("id", variables);
        if (id instanceof String) {
          return new CacheKey(typename, id.toString());
        }
        return null;
      }
    };

    NormalizedCache.configureApolloClientBuilder(apolloClientBuilder, cacheFactory, cacheKeyGenerator, cacheKeyResolver);

    apolloClient = apolloClientBuilder.build();
  }

  private void customScalars() {
    class GeoPoint {
      public final double latitude;
      public final double longitude;

      GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
      }
    }

    ScalarAdapter<GeoPoint> geoPointAdapter = new ScalarAdapter<GeoPoint>() {
      @Override public GeoPoint fromJson(@NotNull JsonReader reader) throws IOException {
        Double latitude = null;
        Double longitude = null;
        reader.beginObject();
        while (reader.hasNext()) {
          switch (reader.nextName()) {
            case "latitude":
              latitude = reader.nextDouble();
              break;
            case "longitude":
              longitude = reader.nextDouble();
              break;
          }
        }
        reader.endObject();
        if (latitude != null && longitude != null) {
          return new GeoPoint(latitude, longitude);
        }
        throw new RuntimeException("Invalid GeoPoint");
      }

      @Override public void toJson(@NotNull JsonWriter writer, GeoPoint value) throws IOException {
        writer.beginObject();
        writer.name("latitude").value(value.latitude);
        writer.name("longitude").value(value.longitude);
        writer.endObject();
      }
    };

    apolloClient = new ApolloClient.Builder()
        .serverUrl("https://localhost")
        .addScalarAdapter(javatest.type.GeoPoint.type, geoPointAdapter)
        .build();
  }

  private void clientAwareness() {
    apolloClient = new ApolloClient.Builder()
        .networkTransport(
            new HttpNetworkTransport.Builder()
                .serverUrl("https://localhost")
                .addInterceptor(new ApolloClientAwarenessInterceptor("clientName", "clientVersion"))
                .build()
        )
        .build();
  }
}
