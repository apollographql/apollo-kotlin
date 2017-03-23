package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.normalized.Record;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

interface CallInterceptor {
  InterceptorResponse intercept(Operation operation, CallInterceptorChain chain) throws IOException;

  void interceptAsync(Operation operation, CallInterceptorChain chain, ExecutorService dispatcher, CallBack callBack);

  void dispose();

  interface CallBack {
    void onResponse(InterceptorResponse response);

    void onFailure(Throwable t);
  }

  final class InterceptorResponse {
    final okhttp3.Response httpResponse;
    final Response parsedResponse;
    final Collection<Record> cacheRecords;

    InterceptorResponse(okhttp3.Response httpResponse) {
      this(httpResponse, null, null);
    }

    InterceptorResponse(okhttp3.Response httpResponse, Response parsedResponse, Collection<Record> cacheRecords) {
      this.httpResponse = httpResponse;
      this.parsedResponse = parsedResponse;
      this.cacheRecords = cacheRecords;
    }
  }
}
