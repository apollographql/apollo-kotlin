package com.apollographql.apollo.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

public interface ApolloInterceptor {
  @Nonnull InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException;

  void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
      @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack);

  void dispose();

  interface CallBack {
    void onResponse(@Nonnull InterceptorResponse response);

    void onFailure(@Nonnull ApolloException e);
  }

  final class InterceptorResponse {
    public final Optional<okhttp3.Response> httpResponse;
    public final Optional<Response> parsedResponse;
    public final Optional<Collection<Record>> cacheRecords;

    public InterceptorResponse(okhttp3.Response httpResponse) {
      this(httpResponse, null, null);
    }

    public InterceptorResponse(okhttp3.Response httpResponse, Response parsedResponse,
        Collection<Record> cacheRecords) {
      this.httpResponse = Optional.fromNullable(httpResponse);
      this.parsedResponse = Optional.fromNullable(parsedResponse);
      this.cacheRecords = Optional.fromNullable(cacheRecords);
    }
  }
}
