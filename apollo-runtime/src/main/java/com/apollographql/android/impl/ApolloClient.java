package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CallAdapter;
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

public final class ApolloClient<R> implements ApolloCall.Factory<R> {
  public static <B> Builder<B> builder() {
    return new Builder<>();
  }

  private final HttpUrl serverUrl;
  private final Call.Factory httpCallFactory;
  private final HttpCache httpCache;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;
  private CallAdapter<R> adapter;

  private ApolloClient(Builder<R> builder) {
    this.serverUrl = builder.serverUrl;
    this.httpCallFactory = builder.okHttpClient;
    this.httpCache = builder.httpCache;
    this.customTypeAdapters = builder.customTypeAdapters;
    this.moshi = builder.moshiBuilder.build();
    this.adapter = builder.callAdapter;
  }

  @Nonnull
  public <T extends Operation> R newCall(@Nonnull T operation) {
    RealApolloCall call = new RealApolloCall(operation, serverUrl, httpCallFactory, httpCache, moshi,
        operation.responseFieldMapper(), customTypeAdapters);
    return adapter.adapt(call);
  }

  public static class Builder<B> {
    OkHttpClient okHttpClient;
    HttpUrl serverUrl;
    HttpCache httpCache;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    Moshi.Builder moshiBuilder = new Moshi.Builder();
    CallAdapter<B> callAdapter;

    private Builder() {
    }

    public Builder<B> okHttpClient(@Nonnull OkHttpClient okHttpClient) {
      this.okHttpClient = checkNotNull(okHttpClient, "okHttpClient is null");
      return this;
    }

    public Builder<B> serverUrl(@Nonnull HttpUrl serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
      return this;

    }

    public Builder<B> serverUrl(@Nonnull String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      this.serverUrl = HttpUrl.parse(baseUrl);
      return this;
    }

    public Builder<B> httpCache(HttpCache httpCache) {
      this.httpCache = httpCache;
      return this;
    }

    public Builder<B> withCallAdapter(@Nonnull CallAdapter<B> callAdapter) {
      checkNotNull(callAdapter, "callAdapter is null");
      this.callAdapter = callAdapter;
      return this;
    }

    public <T> Builder<B> withCustomTypeAdapter(@Nonnull ScalarType scalarType,
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

    public ApolloClient<B> build() {
      checkNotNull(okHttpClient, "okHttpClient is null");
      checkNotNull(serverUrl, "serverUrl is null");
      checkNotNull(callAdapter, "callAdapter is null");

      if (httpCache != null) {
        okHttpClient = okHttpClient.newBuilder().addInterceptor(new HttpCacheInterceptor(httpCache)).build();
      }

      return new ApolloClient<>(this);
    }
  }
}
