package com.apollographql.apollo3.runtime.java.network.http.internal;

import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.ExecutionOptions;
import com.apollographql.apollo3.api.http.HttpBody;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpKt;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.api.http.HttpRequest;
import com.apollographql.apollo3.api.http.HttpResponse;
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloHttpException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.runtime.java.network.http.HttpCallback;
import com.apollographql.apollo3.runtime.java.network.http.HttpInterceptor;
import com.apollographql.apollo3.runtime.java.network.http.HttpInterceptorChain;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.apollographql.apollo3.api.Adapters.AnyAdapter;

public class BatchingHttpInterceptor implements HttpInterceptor {
  private final long batchIntervalMillis;
  private final int maxBatchSize;
  private final boolean exposeErrorBody;

  private @NotNull HttpInterceptorChain interceptorChain;
  private final List<PendingRequest> pendingRequests = new ArrayList<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public BatchingHttpInterceptor(long batchIntervalMillis, int maxBatchSize, boolean exposeErrorBody) {
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
    private final HttpRequest request;
    private final CountDownLatch latch = new CountDownLatch(1);
    private HttpResponse response;
    private ApolloNetworkException exception;

    private PendingRequest(HttpRequest request) {
      this.request = request;
    }

    private void setResponse(HttpResponse response) {
      this.response = response;
      latch.countDown();
    }

    private void setException(ApolloNetworkException exception) {
      this.exception = exception;
      latch.countDown();
    }

    private HttpResponse getResponse() throws ApolloNetworkException {
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

  @Override public void intercept(@NotNull HttpRequest request, @NotNull HttpInterceptorChain chain, @NotNull HttpCallback callback) {
    // Batching is enabled by default, unless explicitly disabled
    String canBeBatchedHeader = HttpKt.get(request.getHeaders(), ExecutionOptions.CAN_BE_BATCHED);
    boolean canBeBatched = canBeBatchedHeader == null || Boolean.parseBoolean(canBeBatchedHeader);
    if (!canBeBatched) {
      // Remove the CAN_BE_BATCHED header and forward directly
      List<HttpHeader> headers = request.getHeaders().stream()
          .filter(header -> !ExecutionOptions.CAN_BE_BATCHED.equals(header.getName()))
          .collect(Collectors.toList());
      chain.proceed(request.newBuilder().headers(headers).build(), callback);
      return;
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
      callback.onResponse(pendingRequest.getResponse());
    } catch (ApolloNetworkException e) {
      callback.onFailure(e);
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

    HttpRequest firstRequest = pending.get(0).request;
    List<HttpBody> allBodies = pending.stream().map(p -> p.request.getBody()).collect(Collectors.toList());
    // Only keep headers with the same name and value in all requests
    List<HttpHeader> commonHeaders = pending.stream().map(p -> p.request.getHeaders()).reduce((acc, headers) -> {
      List<HttpHeader> common = new ArrayList<>();
      for (HttpHeader accHeader : acc) {
        String headerValue = HttpKt.get(headers, accHeader.getName());
        if (headerValue != null && headerValue.equals(accHeader.getValue())) {
          common.add(accHeader);
        }
      }
      return common;
    }).get();
    // Also do not send our internal use header
    commonHeaders = commonHeaders.stream().filter(header -> !ExecutionOptions.CAN_BE_BATCHED.equals(header.getName())).collect(Collectors.toList());

    HttpBody body = new HttpBody() {
      @NotNull @Override public String getContentType() {
        return "application/json";
      }

      @Override public long getContentLength() {
        // We don't know the combined size at that point.
        return -1;
      }

      @Override public void writeTo(@NotNull BufferedSink bufferedSink) {
        BufferedSinkJsonWriter writer = new BufferedSinkJsonWriter(bufferedSink);
        writer.beginArray();
        for (HttpBody requestBody : allBodies) {
          Buffer buffer = new Buffer();
          requestBody.writeTo(buffer);
          writer.jsonValue(buffer.readUtf8());
        }
        writer.endArray();
      }
    };

    HttpRequest request = new HttpRequest.Builder(HttpMethod.Post, firstRequest.getUrl())
        .body(body)
        .headers(commonHeaders)
        .build();

    interceptorChain.proceed(request, new HttpCallback() {
      @Override public void onResponse(@NotNull HttpResponse response) {
        try {
          if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
            BufferedSource maybeBody;
            BufferedSource responseBody = response.getBody();
            if (exposeErrorBody) {
              maybeBody = responseBody;
            } else {
              if (responseBody != null) {
                try {
                  responseBody.close();
                } catch (IOException ignored) {
                }
              }
              maybeBody = null;
            }
            throw new ApolloNetworkException(null, new ApolloHttpException(response.getStatusCode(), response.getHeaders(), maybeBody, "HTTP error " + response.getStatusCode() + " while executing batched query", null));
          }
          BufferedSource responseBody = response.getBody();
          if (responseBody == null) {
            throw new ApolloNetworkException("null body when executing batched query", null);
          }

          Object fromJson;
          try {
            fromJson = AnyAdapter.fromJson(new BufferedSourceJsonReader(responseBody), CustomScalarAdapters.Empty);
          } catch (IOException e) {
            throw new ApolloNetworkException("failed to parse batched response JSON", e);
          }
          if (!(fromJson instanceof List)) {
            throw new ApolloNetworkException("batched query response is not a list when executing batched query", null);
          }
          List<?> list = (List<?>) fromJson;
          if (list.size() != pending.size()) {
            throw new ApolloNetworkException("batched query response count (" + list.size() + ") does not match the requested queries (" + pending.size() + ")", null);
          }
          ArrayList<ByteString> result = new ArrayList<>();
          for (Object o : list) {
            if (o == null) {
              throw new ApolloNetworkException("batched query response contains a null item", null);
            }
            result.add(toJsonByteString(o));
          }
          for (int i = 0; i < result.size(); i++) {
            // This works because the server must return the responses in order
            pending.get(i).setResponse(new HttpResponse.Builder(200)
                .body(result.get(i))
                .build()
            );
          }

        } catch (ApolloNetworkException e) {
          for (PendingRequest p : pending) {
            p.setException(e);
          }
        }
      }

      @Override public void onFailure(@NotNull ApolloNetworkException exception) {
        for (PendingRequest p : pending) {
          p.setException(exception);
        }
      }
    });
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
