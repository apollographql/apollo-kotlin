package com.apollographql.apollo.runtime.java.network.http.internal;

import com.apollographql.apollo.api.http.HttpMethod;
import com.apollographql.apollo.api.http.HttpRequest;
import com.apollographql.apollo.api.http.HttpResponse;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import com.apollographql.apollo.runtime.java.network.http.HttpCallback;
import com.apollographql.apollo.runtime.java.network.http.HttpEngine;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttpHttpEngine implements HttpEngine {
  private final Call.Factory okHttpCallFactory;

  public OkHttpHttpEngine(Call.Factory okHttpCallFactory) {
    this.okHttpCallFactory = okHttpCallFactory;
  }

  public OkHttpHttpEngine(long connectTimeout, long readTimeout) {
    this.okHttpCallFactory = new OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
        .build();
  }

  @Override public void execute(@NotNull HttpRequest request, @NotNull HttpCallback callback, @NotNull ApolloDisposable disposable) {
    Request.Builder okHttpRequestBuilder = new Request.Builder().url(request.getUrl());
    request.getHeaders().forEach(httpHeader -> {
          okHttpRequestBuilder.addHeader(httpHeader.getName(), httpHeader.getValue());
        }
    );

    if (request.getMethod() == HttpMethod.Post) {
      okHttpRequestBuilder.post(new RequestBody() {
        @Nullable @Override public MediaType contentType() {
          return MediaType.parse(request.getBody().getContentType());
        }

        @Override public long contentLength() {
          return request.getBody().getContentLength();
        }

        @Override public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
          request.getBody().writeTo(bufferedSink);
        }
      });
    }
    Call okHttpCall = okHttpCallFactory.newCall(okHttpRequestBuilder.build());
    ApolloDisposable.Listener listener = okHttpCall::cancel;
    disposable.addListener(listener);
    try {
      // It might be that we are cancelled even before we registered the listener
      // Do an early check for that case
      if (disposable.isDisposed()) return;
      Response okHttpResponse = okHttpCall.execute();
      if (disposable.isDisposed()) return;
      HttpResponse.Builder httpResponseBuilder = new HttpResponse.Builder(okHttpResponse.code())
          .body(okHttpResponse.body().source());
      okHttpResponse.headers().forEach(pair -> httpResponseBuilder.addHeader(pair.getFirst(), pair.getSecond()));
      callback.onResponse(httpResponseBuilder.build());
    } catch (IOException e) {
      if (disposable.isDisposed()) return;
      callback.onFailure(new ApolloNetworkException("Network error", e));
    } finally {
      disposable.removeListener(listener);
    }
  }

  @Override public void dispose() {
  }
}
