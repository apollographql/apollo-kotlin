package com.apollographql.apollo3.runtime.java.network.http;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Operations;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.api.http.HttpRequest;
import com.apollographql.apollo3.api.http.HttpRequestComposer;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloHttpException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.ApolloParseException;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.network.NetworkTransport;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;

public class HttpNetworkTransport implements NetworkTransport {
  private Call.Factory callFactory;
  private HttpRequestComposer requestComposer;

  public HttpNetworkTransport(Call.Factory callFactory, HttpRequestComposer httpRequestComposer) {
    this.callFactory = callFactory;
    this.requestComposer = httpRequestComposer;
  }

  @Override
  public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {
    HttpRequest httpRequest = requestComposer.compose(request);
    Request.Builder builder = new Request.Builder()
        .url(httpRequest.getUrl());

    httpRequest.getHeaders().forEach(httpHeader -> {
          builder.addHeader(httpHeader.getName(), httpHeader.getValue());
        }
    );

    if (httpRequest.getMethod() == HttpMethod.Post) {
      builder.post(new RequestBody() {
        @Nullable @Override public MediaType contentType() {
          return MediaType.parse(httpRequest.getBody().getContentType());
        }

        @Override public long contentLength() {
          return httpRequest.getBody().getContentLength();
        }

        @Override public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
          httpRequest.getBody().writeTo(bufferedSink);
        }
      });
    }

    Call call = callFactory.newCall(builder.build());

    ApolloDisposable.Listener listener = () -> call.cancel();

    disposable.addListener(listener);

    try {
      // It might be that we are cancelled even before we registered the listener
      // Do an early check for that case
      if (disposable.isDisposed()) {
        return;
      }
      Response response = call.execute();
      if (!response.isSuccessful()) {
        ArrayList<HttpHeader> headers = new ArrayList<>();
        response.headers().forEach(pair -> headers.add(new HttpHeader(pair.getFirst(), pair.getSecond())));

        // TODO: include body in exception
        callback.onFailure(new ApolloHttpException(response.code(), headers, null, "HTTP error", null));
      } else {
        BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(response.body().source());
        try {
          CustomScalarAdapters customScalarAdapters = request.getExecutionContext().get(CustomScalarAdapters.Key);
          ApolloResponse<D> apolloResponse = Operations.parseJsonResponse(request.getOperation(), jsonReader, customScalarAdapters);
          callback.onResponse(apolloResponse);
        } catch (Exception e) {
          if (disposable.isDisposed()) {
            return;
          }
          callback.onFailure(new ApolloParseException("Cannot parse response", e));
        }
      }
    } catch (IOException e) {
      if (disposable.isDisposed()) {
        return;
      }
      callback.onFailure(new ApolloNetworkException("Network error", e));
    } finally {
      disposable.removeListener(listener);
    }
  }
}
