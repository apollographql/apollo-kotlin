package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static com.apollographql.android.Utils.checkNotNull;

/** TODO */
public final class ApolloClient implements ApolloCallFactory {
  public static Builder builder() {
    return new Builder();
  }

  private final HttpUrl baseUrl;
  private final Call.Factory httpCallFactory;
  private final Map<Type, ResponseFieldMapper> responseFieldMappers;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;

  private ApolloClient(HttpUrl baseUrl, Call.Factory httpCallFactory, Map<Type, ResponseFieldMapper> responseFieldMappers,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Moshi moshi) {
    this.baseUrl = baseUrl;
    this.httpCallFactory = httpCallFactory;
    this.responseFieldMappers = responseFieldMappers;
    this.customTypeAdapters = customTypeAdapters;
    this.moshi = moshi;
  }

  public <T extends Operation> ApolloCall newCall(@Nonnull T operation) {
    ResponseFieldMapper responseFieldMapper = responseFieldMappers.get(operation.getClass());
    if (responseFieldMapper == null) {
      throw new RuntimeException("failed to resolve response field mapper for: " + operation.getClass());
    }
    return new RealApolloCall(operation, baseUrl, httpCallFactory, moshi, responseFieldMapper, customTypeAdapters);
  }

  public static class Builder {
    private OkHttpClient okHttpClient;
    private HttpUrl baseUrl;
    private final Map<Type, ResponseFieldMapper> responseFieldMappers = new LinkedHashMap<>();
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    private Moshi.Builder moshiBuilder = new Moshi.Builder();

    public Builder okHttpClient(@Nonnull OkHttpClient okHttpClient) {
      this.okHttpClient = checkNotNull(okHttpClient, "okHttpClient == null");
      return this;
    }

    public Builder baseUrl(@Nonnull HttpUrl baseUrl) {
      this.baseUrl = checkNotNull(baseUrl, "baseUrl == null");
      return this;
    }

    public Builder baseUrl(@Nonnull String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      this.baseUrl = HttpUrl.parse(baseUrl);
      return this;
    }

    public <T> Builder withCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      moshiBuilder.add(scalarType.javaType(), new JsonAdapter<T>() {
        @Override public T fromJson(com.squareup.moshi.JsonReader reader) throws IOException {
          return customTypeAdapter.decode(reader.nextString());
        }

        @Override public void toJson(JsonWriter writer, T value) throws IOException {
          //noinspection unchecked
          writer.value(customTypeAdapter.encode(value));
        }
      });
      return this;
    }

    public <T> Builder withResponseFieldMapper(@Nonnull Class type,
        @Nonnull ResponseFieldMapper<T> responseFieldMapper) {
      responseFieldMappers.put(type, responseFieldMapper);
      return this;
    }

    public Builder withResponseFieldMappers(@Nonnull Map<Type, ResponseFieldMapper> mappers) {
      responseFieldMappers.putAll(mappers);
      return this;
    }

    public ApolloClient build() {
      checkNotNull(okHttpClient, "okHttpClient == null");
      checkNotNull(baseUrl, "baseUrl == null");
      return new ApolloClient(baseUrl, okHttpClient, responseFieldMappers, customTypeAdapters, moshiBuilder.build());
    }
  }
}
