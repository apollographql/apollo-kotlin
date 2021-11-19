package test;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CompiledField;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Executable;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.JsonWriter;
import com.apollographql.apollo3.cache.http.HttpCacheExtensions;
import com.apollographql.apollo3.cache.http.HttpFetchPolicy;
import com.apollographql.apollo3.cache.normalized.NormalizedCacheExtensions;
import com.apollographql.apollo3.cache.normalized.api.CacheKey;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyResolver;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.ObjectIdGenerator;
import com.apollographql.apollo3.cache.normalized.api.ObjectIdGeneratorContext;
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.mockserver.MockServerKt;
import com.apollographql.apollo3.network.http.ApolloClientAwarenessInterceptor;
import com.apollographql.apollo3.network.http.BatchingHttpEngine;
import com.apollographql.apollo3.network.http.BatchingHttpEngineExtensions;
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
import java.util.Map;

public class ClientTest {
  MockServer mockServer;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = MockServerKt.MockServer();

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
    mockServer.enqueue(new MockResponse("{\"data\": {\"random\": 42}}"));
    ApolloResponse<GetRandomQuery.Data> queryResponse = Rx2Apollo.single(
        apolloClient.query(new GetRandomQuery())
    ).blockingGet();
    Truth.assertThat(queryResponse.dataAssertNoErrors().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}"));
    ApolloResponse<CreateCatMutation.Data> mutationResponse = Rx2Apollo.single(
        apolloClient.mutate(new CreateCatMutation())
    ).blockingGet();
    Truth.assertThat(mutationResponse.dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");

    Disposable disposable = Rx2Apollo.flowable(
        apolloClient.subscribe(new AnimalCreatedSubscription())
    ).subscribe(result -> {
      String species = result.dataAssertNoErrors().animalCreated.catFragment.species;
    });
  }

  private void autoPersistedQueries() {
    apolloClient = new ApolloClient.Builder()
        .serverUrl("https://localhost")
        .autoPersistedQueries()
        .build();
  }

  private void queryBatching() {
    apolloClient = BatchingHttpEngineExtensions.canBeBatched(
        new ApolloClient.Builder()
            .serverUrl("https://localhost")
            .httpEngine(new BatchingHttpEngine()),
        false
    ).build();

    ApolloResponse<GetRandomQuery.Data> result = Rx2Apollo.single(BatchingHttpEngineExtensions.canBeBatched(
        apolloClient.query(new GetRandomQuery()),
        true
    )).blockingGet();
  }

  private void httpCache() {
    File cacheDir = new File("/tmp/apollo-cache");
    long cacheSize = 10_000_000;
    apolloClient = HttpCacheExtensions.httpCache(
        new ApolloClient.Builder().serverUrl("https://localhost"),
        cacheDir,
        cacheSize
    ).build();

    ApolloResponse<GetRandomQuery.Data> result = Rx2Apollo.single(HttpCacheExtensions.httpFetchPolicy(
        apolloClient.query(new GetRandomQuery()),
        HttpFetchPolicy.NetworkOnly
    )).blockingGet();
  }

  private void normalizedCache() {
    NormalizedCacheFactory cacheFactory = new MemoryCacheFactory().chain(
        new SqlNormalizedCacheFactory("jdbc:sqlite:apollo.db")
    );

    // Using default objectIdGenerator/cacheResolver
    apolloClient = NormalizedCacheExtensions.normalizedCache(
        new ApolloClient.Builder().serverUrl("https://localhost"),
        cacheFactory
    ).build();

    // Using custom objectIdGenerator/cacheResolver

    ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator() {
      @Override public CacheKey cacheKeyForObject(@NotNull Map<String, ?> obj, @NotNull ObjectIdGeneratorContext context) {
        return new CacheKey(obj.get("id").toString());
      }
    };

    CacheKeyResolver cacheKeyResolver = new CacheKeyResolver() {
      @Override public CacheKey cacheKeyForField(@NotNull CompiledField field, @NotNull Executable.Variables variables) {
        String typename = field.getType().leafType().getName();
        Object id = field.resolveArgument("id", variables);
        if (id instanceof String) {
          return new CacheKey(typename, id.toString());
        }
        return null;
      }
    };

    apolloClient = NormalizedCacheExtensions.normalizedCache(
        new ApolloClient.Builder().serverUrl("https://localhost"),
        cacheFactory,
        objectIdGenerator,
        cacheKeyResolver
    ).build();
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

    Adapter<GeoPoint> geoPointAdapter = new Adapter<GeoPoint>() {
      @Override public GeoPoint fromJson(@NotNull JsonReader reader, @NotNull CustomScalarAdapters customScalarAdapters) {
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

      @Override public void toJson(@NotNull JsonWriter writer, @NotNull CustomScalarAdapters customScalarAdapters, GeoPoint value) {
        writer.beginObject();
        writer.name("latitude").value(value.latitude);
        writer.name("longitude").value(value.longitude);
        writer.endObject();
      }
    };

    apolloClient = new ApolloClient.Builder()
        .serverUrl("https://localhost")
        .addCustomScalarAdapter(javatest.type.GeoPoint.type, geoPointAdapter)
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
