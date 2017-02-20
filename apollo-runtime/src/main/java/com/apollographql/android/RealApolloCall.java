package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

final class RealApolloCall implements ApolloCall {
  private static final String ACCEPT_TYPE = "application/json";
  private static final String CONTENT_TYPE = "application/json";
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  private final Operation operation;
  private final HttpUrl serverUrl;
  private final okhttp3.Call.Factory httpCallFactory;
  private final Moshi moshi;
  private final ResponseBodyConverter responseBodyConverter;
  private volatile Call httpCall;
  private boolean executed;

  RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, Moshi moshi,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.moshi = moshi;
    this.httpCallFactory = httpCallFactory;
    this.responseBodyConverter = new ResponseBodyConverter(operation, responseFieldMapper, customTypeAdapters);
  }

  private RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, Moshi moshi,
      ResponseBodyConverter responseBodyConverter) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.moshi = moshi;
    this.httpCallFactory = httpCallFactory;
    this.responseBodyConverter = responseBodyConverter;
  }

  @Override public void cancel() {
    Call call = httpCall;
    if (call != null) {
      call.cancel();
    }
  }

  @Override @Nonnull public <T extends Operation.Data> Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    httpCall = httpCallFactory.newCall(httpRequest(operation));
    return parseHttpResponse(httpCall.execute());
  }

  @Override @Nonnull public <T extends Operation.Data> ApolloCall enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    Request request;
    try {
      request = httpRequest(operation);
    } catch (Exception e) {
      if (callback != null) {
        callback.onFailure(e);
      }
      return this;
    }

    httpCall = httpCallFactory.newCall(request);
    httpCall.enqueue(new okhttp3.Callback() {
      @Override public void onFailure(Call call, IOException e) {
        if (callback != null) {
          callback.onFailure(e);
        }
      }

      @Override public void onResponse(Call call, okhttp3.Response httpResponse) throws IOException {
        try {
          Response<T> response = parseHttpResponse(httpResponse);
          if (callback != null) {
            callback.onResponse(response);
          }
        } catch (Exception e) {
          if (callback != null) {
            callback.onFailure(e);
          }
        }
      }
    });
    return this;
  }

  @Override @Nonnull public ApolloCall clone() {
    return new RealApolloCall(operation, serverUrl, httpCallFactory, moshi, responseBodyConverter);
  }

  private <T extends Operation.Data> Response<T> parseHttpResponse(okhttp3.Response response) throws IOException {
    int code = response.code();
    if (code < 200 || code >= 300) {
      throw new HttpException(response);
    } else {
      return responseBodyConverter.convert(response.body());
    }
  }

  private Request httpRequest(Operation operation) throws IOException {
    RequestBody requestBody = httpRequestBody(operation);
    return new Request.Builder()
        .url(serverUrl)
        .post(requestBody)
        .header("Accept", ACCEPT_TYPE)
        .header("Content-Type", CONTENT_TYPE)
        .build();
  }

  private RequestBody httpRequestBody(Operation operation) throws IOException {
    JsonAdapter<Operation> adapter = new OperationJsonAdapter(moshi);
    Buffer buffer = new Buffer();
    adapter.toJson(buffer, operation);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
