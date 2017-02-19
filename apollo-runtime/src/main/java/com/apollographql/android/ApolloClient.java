package com.apollographql.android;

import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static com.apollographql.android.Utils.checkNotNull;

/** TODO */
public final class ApolloClient {
  private final HttpOperationRequestFactory httpRequestFactory;

  public static Builder builder() {
    return new Builder();
  }

  private ApolloClient(HttpUrl baseUrl, Call.Factory callFactory, Map<Type, ResponseFieldMapper> responseFieldMappers,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    httpRequestFactory = new HttpOperationRequestFactory(baseUrl, callFactory, responseFieldMappers,
        customTypeAdapters);
  }

  public <T extends Query.Data> OperationRequest<T> fetch(@Nonnull Query query, OperationRequest.Callback callback) {
    //noinspection unchecked
    return httpRequestFactory.createRequest(query).enqueue(callback);
  }

  public <T extends Mutation.Data> OperationRequest<T> perform(@Nonnull Mutation mutation,
      OperationRequest.Callback callback) {
    //noinspection unchecked
    return httpRequestFactory.createRequest(mutation).enqueue(callback);
  }

  public <T extends Operation.Data> OperationRequest<T> request(@Nonnull Operation operation) {
    return httpRequestFactory.createRequest(operation);
  }

  public static class Builder {
    private OkHttpClient okHttpClient;
    private HttpUrl baseUrl;
    private final Map<Type, ResponseFieldMapper> responseFieldMappers = new LinkedHashMap<>();
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();

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
      return new ApolloClient(baseUrl, okHttpClient, responseFieldMappers, customTypeAdapters);
    }
  }
}
