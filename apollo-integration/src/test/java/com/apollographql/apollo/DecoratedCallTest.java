package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Timeout;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class DecoratedCallTest {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloCall.Callback<AllPlanetsQuery.Data> callback;

  @Before
  public void setup() {
    final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(Utils.INSTANCE.immediateExecutorService()))
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .callFactory(new Call.Factory() {
          @NotNull @Override public Call newCall(@NotNull Request request) {
            return new DecoratedCall(okHttpClient.newCall(request));
          }
        })
        .dispatcher(Utils.INSTANCE.immediateExecutor())
        .build();

    callback = spy(new ApolloCall.Callback<AllPlanetsQuery.Data>() {
      @Override public void onResponse(@NotNull Response<AllPlanetsQuery.Data> response) { }

      @Override public void onFailure(@NotNull ApolloException e) { }
    });
  }

  @Test public void onResponseCallbackInvokedWithDecoratedCall() throws Exception {
    final MockResponse response = new MockResponse().setChunkedBody(Utils.INSTANCE.readFileToString(getClass(),
        "/" + "HttpCacheTestAllPlanets.json"), 32);
    server.enqueue(response);
    apolloClient.query(new AllPlanetsQuery())
        .enqueue(callback);
    verify(callback).onResponse(Matchers.<Response<AllPlanetsQuery.Data>>any());
  }

  @Test public void onFailureCallbackInvokedWithDecoratedCall() {
    server.enqueue(new MockResponse().setResponseCode(500));
    apolloClient.query(new AllPlanetsQuery())
        .enqueue(callback);
    verify(callback).onFailure(any(ApolloException.class));
  }

  private static final class DecoratedCall implements Call {

    private final Call call;

    DecoratedCall(Call call) {
      this.call = call;
    }

    @NotNull @Override public Request request() {
      return call.request();
    }

    @NotNull @Override public okhttp3.Response execute() throws IOException {
      return call.execute();
    }

    @Override public void enqueue(@NotNull Callback responseCallback) {
      call.enqueue(responseCallback);
    }

    @Override public void cancel() {
      call.cancel();
    }

    @Override public boolean isExecuted() {
      return call.isExecuted();
    }

    @Override public boolean isCanceled() {
      return call.isCanceled();
    }

    @NotNull @Override public Timeout timeout() {
      return call.timeout();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @NotNull @Override public Call clone() {
      return new DecoratedCall(call.clone());
    }
  }
}