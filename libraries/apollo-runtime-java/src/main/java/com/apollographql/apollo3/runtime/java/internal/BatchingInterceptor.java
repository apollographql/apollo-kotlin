package com.apollographql.apollo3.runtime.java.internal;

import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.ExecutionOptions;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.exception.ApolloHttpException;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.apollographql.apollo3.api.Adapters.AnyAdapter;

public class BatchingInterceptor implements Interceptor {
  private final long batchIntervalMillis;
  private final int maxBatchSize;
  private final boolean exposeErrorBody;

  private Chain interceptorChain;
  private final List<PendingRequest> pendingRequests = new ArrayList<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Object batchLoopLock = new Object();

  public BatchingInterceptor(long batchIntervalMillis, int maxBatchSize, boolean exposeErrorBody) {
    this.batchIntervalMillis = batchIntervalMillis;
    this.maxBatchSize = maxBatchSize;
    this.exposeErrorBody = exposeErrorBody;
  }

  private void scheduleExecutePendingRequests() {
    executor.execute(() -> {
      try {
        Thread.sleep(batchIntervalMillis);
      } catch (InterruptedException ignored) {
      }
      executePendingRequests();
    });
  }

  private static class PendingRequest {
    private final Request request;
    private final CountDownLatch latch = new CountDownLatch(1);
    private Response response;
    private ApolloException exception;

    private PendingRequest(Request request) {
      this.request = request;
    }

    private void setResponse(Response response) {
      this.response = response;
      latch.countDown();
    }

    private void setException(ApolloException exception) {
      this.exception = exception;
      latch.countDown();
    }

    private Response getResponse() throws ApolloException {
      try {
        latch.await();
      } catch (InterruptedException ignored) {
      }
      if (exception != null) {
        throw exception;
      }
      return response;
    }
  }

  @NotNull @Override public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
    Request request = chain.request();
    // Batching is enabled by default, unless explicitly disabled
    String canBeBatchedHeader = request.header(ExecutionOptions.CAN_BE_BATCHED);
    boolean canBeBatched = canBeBatchedHeader == null || Boolean.parseBoolean(canBeBatchedHeader);
    if (!canBeBatched) {
      // Remove the CAN_BE_BATCHED header and forward directly
      return chain.proceed(request.newBuilder().removeHeader(ExecutionOptions.CAN_BE_BATCHED).build());
    }

    // Keep the chain for later
    interceptorChain = chain;

    PendingRequest pendingRequest = new PendingRequest(request);
    int pendingRequestsSize;
    synchronized (pendingRequests) {
      pendingRequests.add(pendingRequest);
      pendingRequestsSize = pendingRequests.size();
    }
    if (pendingRequestsSize >= maxBatchSize) {
      executePendingRequests();
    } else {
      scheduleExecutePendingRequests();
    }

    try {
      return pendingRequest.getResponse();
    } catch (ApolloException e) {
      throw new IOException(e);
    }
  }

  private void executePendingRequests() {
    List<PendingRequest> pending;
    synchronized (pendingRequests) {
      pending = new ArrayList<>(pendingRequests);
      pendingRequests.clear();
    }

    if (pending.isEmpty()) {
      return;
    }

    Request firstRequest = pending.get(0).request;
    List<RequestBody> allBodies = pending.stream().map(p -> p.request.body()).collect(Collectors.toList());
    // Only keep headers with the same name and value in all requests
    Headers commonHeaders = pending.stream().map(p -> p.request.headers()).reduce((acc, headers) -> {
      Headers.Builder accBuilder = acc.newBuilder();
      for (String name : acc.names()) {
        String headerValue = headers.get(name);
        if (headerValue == null || !headerValue.equals(acc.get(name))) {
          accBuilder.removeAll(name);
        }
      }
      return accBuilder.build();
    }).get();
    // Also do not send our internal use header
    commonHeaders = commonHeaders.newBuilder().removeAll(ExecutionOptions.CAN_BE_BATCHED).build();

    RequestBody body = new RequestBody() {
      @Nullable @Override public MediaType contentType() {
        return MediaType.parse("application/json");
      }

      @Override public long contentLength() {
        // We don't know the combined size at that point.
        return -1;
      }

      @Override public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        BufferedSinkJsonWriter writer = new BufferedSinkJsonWriter(bufferedSink);
        writer.beginArray();
        for (RequestBody requestBody : allBodies) {
          Buffer buffer = new Buffer();
          requestBody.writeTo(buffer);
          writer.jsonValue(buffer.readUtf8());
        }
        writer.endArray();
      }
    };

    Request okHttpRequest = new Request.Builder().url(firstRequest.url()).headers(commonHeaders).post(body).build();

    ApolloException exception = null;
    List<ByteString> result = null;
    Response response = null;
    try {
      response = interceptorChain.proceed(okHttpRequest);
      if (response.code() < 200 || response.code() > 299) {
        BufferedSource maybeBody;
        ResponseBody responseBody = response.body();
        if (exposeErrorBody) {
          maybeBody = responseBody == null ? null : responseBody.source();
        } else {
          if (responseBody != null) responseBody.close();
          maybeBody = null;
        }
        throw new ApolloHttpException(response.code(), toHttpHeaders(response.headers()), maybeBody, "HTTP error " + response.code() + " while executing batched query", null);
      }
      ResponseBody responseBody = response.body();
      if (responseBody == null) {
        throw new ApolloException("null body when executing batched query", null);
      }

      Object fromJson = AnyAdapter.fromJson(new BufferedSourceJsonReader(responseBody.source()), CustomScalarAdapters.Empty);
      if (!(fromJson instanceof List)) {
        throw new ApolloException("batched query response is not a list when executing batched query", null);
      }
      List<?> list = (List<?>) fromJson;
      if (list.size() != pending.size()) {
        throw new ApolloException("batched query response count (" + list.size() + ") does not match the requested queries (" + pending.size() + ")", null);
      }
      result = new ArrayList<>();
      for (Object o : list) {
        if (o == null) {
          throw new ApolloException("batched query response contains a null item", null);
        }
        result.add(toJsonByteString(o));
      }
    } catch (Exception e) {
      exception = e instanceof ApolloException ? (ApolloException) e : new ApolloException("batched query failed with exception", e);
    }
    if (exception != null) {
      for (PendingRequest p : pending) {
        p.setException(exception);
      }
    } else {
      for (int i = 0; i < result.size(); i++) {
        // This works because the server must return the responses in order
        pending.get(i).setResponse(
            new Response.Builder()
                .protocol(response.protocol())
                .code(response.code())
                .message(response.message())
                .request(okHttpRequest)
                .body(ResponseBody.create(result.get(i), MediaType.parse("application/json")))
                .build()
        );
      }
    }
  }

  private static List<HttpHeader> toHttpHeaders(Headers headers) {
    List<HttpHeader> httpHeaders = new ArrayList<>(headers.size());
    for (String name : headers.names()) {
      for (String value : headers.values(name)) {
        httpHeaders.add(new HttpHeader(name, value));
      }
    }
    return httpHeaders;
  }

  private static ByteString toJsonByteString(Object o) {
    Buffer buffer = new Buffer();
    BufferedSinkJsonWriter writer = new BufferedSinkJsonWriter(buffer);
    try {
      AnyAdapter.toJson(writer, CustomScalarAdapters.Empty, o);
    } catch (IOException ignored) {
    }
    return buffer.readByteString();
  }
}
