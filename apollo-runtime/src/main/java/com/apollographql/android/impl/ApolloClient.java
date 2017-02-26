package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.HttpCache;
import com.apollographql.android.cache.HttpCacheInterceptor;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static com.apollographql.android.impl.util.Utils.checkNotNull;

public final class ApolloClient implements ApolloCall.Factory {
  public static Builder builder() {
    return new Builder();
  }

  private final HttpUrl serverUrl;
  private final Call.Factory httpCallFactory;
  private final HttpCache httpCache;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;

  private ApolloClient(Builder builder) {
    this.serverUrl = builder.serverUrl;
    this.httpCallFactory = builder.okHttpClient;
    this.httpCache = builder.httpCache;
    this.customTypeAdapters = builder.customTypeAdapters;
    this.moshi = builder.moshiBuilder.build();
  }


  @Override
  public <D extends Operation.Data, V extends Operation.Variables>
  ApolloCall<D> newCall(@Nonnull Operation<D, V> operation) {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, moshi,
        operation.responseFieldMapper(), customTypeAdapters);
  }

  public static class Builder {
    OkHttpClient okHttpClient;
    HttpUrl serverUrl;
    HttpCache httpCache;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    Moshi.Builder moshiBuilder = new Moshi.Builder();

    private Builder() {
    }

    public Builder okHttpClient(@Nonnull OkHttpClient okHttpClient) {
      this.okHttpClient = checkNotNull(okHttpClient, "okHttpClient is null");
      return this;
    }

    public Builder serverUrl(@Nonnull HttpUrl serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
      return this;

    }

    public Builder serverUrl(@Nonnull String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      this.serverUrl = HttpUrl.parse(baseUrl);
      return this;
    }

    public Builder httpCache(HttpCache httpCache) {
      this.httpCache = httpCache;
      return this;
    }


    public <T> Builder withCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      moshiBuilder.add(scalarType.javaType(), new JsonAdapter<T>() {
        @Override
        public T fromJson(com.squareup.moshi.JsonReader reader) throws IOException {
          return customTypeAdapter.decode(reader.nextString());
        }

        @Override
        public void toJson(JsonWriter writer, T value) throws IOException {
          //noinspection unchecked
          writer.value(customTypeAdapter.encode(value));
        }
      });
      return this;
    }

    public ApolloClient build() {
      checkNotNull(okHttpClient, "okHttpClient is null");
      checkNotNull(serverUrl, "serverUrl is null");

      if (httpCache != null) {
        okHttpClient = okHttpClient.newBuilder().addInterceptor(new HttpCacheInterceptor(httpCache)).build();
      }

      return new ApolloClient(this);
    }
  }
}
