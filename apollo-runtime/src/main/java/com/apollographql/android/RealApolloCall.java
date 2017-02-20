package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

final class RealApolloCall implements ApolloCall {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/graphql; charset=utf-8");

  private final Operation operation;
  private final Moshi moshi;
  private final Request baseRequest;
  private final okhttp3.Call.Factory httpCallFactory;
  private final ResponseBodyConverter responseBodyConverter;
  private Call httpCall;
  private boolean executed;

  RealApolloCall(Operation operation, Moshi moshi, okhttp3.Call.Factory httpCallFactory, Request baseRequest,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.operation = operation;
    this.moshi = moshi;
    this.httpCallFactory = httpCallFactory;
    this.baseRequest = baseRequest;
    this.responseBodyConverter = new ResponseBodyConverter(operation, responseFieldMapper, customTypeAdapters);
  }

  private RealApolloCall(Operation operation, Moshi moshi, okhttp3.Call.Factory httpCallFactory, Request baseRequest,
      ResponseBodyConverter responseBodyConverter) {
    this.operation = operation;
    this.moshi = moshi;
    this.httpCallFactory = httpCallFactory;
    this.baseRequest = baseRequest;
    this.responseBodyConverter = responseBodyConverter;
  }

  @Override public void cancel() {
    Call call = httpCall;
    if (call != null) {
      call.cancel();
    }
  }

  @Override public <T extends Operation.Data> Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    RequestBody requestBody = requestBody(operation);
    Request request = baseRequest.newBuilder().post(requestBody).build();
    httpCall = httpCallFactory.newCall(request);
    return parse(httpCall.execute());
  }

  @Override public <T extends Operation.Data> ApolloCall enqueue(final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    Request request;
    try {
      RequestBody requestBody = requestBody(operation);
      request = baseRequest.newBuilder().post(requestBody).build();
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
          Response<T> response = parse(httpResponse);
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

  @Override public ApolloCall clone() {
    return new RealApolloCall(operation, moshi, httpCallFactory, baseRequest, responseBodyConverter);
  }

  private <T extends Operation.Data> Response<T> parse(okhttp3.Response response) throws IOException {
    int code = response.code();
    if (code < 200 || code >= 300) {
      throw new HttpException(response);
    } else {
      return responseBodyConverter.convert(response.body());
    }
  }

  private RequestBody requestBody(Operation operation) throws IOException {
    JsonAdapter<Operation> adapter = new OperationJsonAdapter(moshi);
    Buffer buffer = new Buffer();
    adapter.toJson(buffer, operation);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
