package com.apollographql.apollo;

import com.google.common.io.CharStreams;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Predicate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_ONLY;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;

public final class Utils {
  public static final long TIME_OUT_SECONDS = 3;

  private Utils() {
  }

  public static String readFileToString(final Class contextClass,
      final String streamIdentifier) throws IOException {

    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(contextClass.getResourceAsStream(streamIdentifier), Charset.defaultCharset());
      return CharStreams.toString(inputStreamReader);
    } catch (IOException e) {
      throw new IOException();
    } finally {
      if (inputStreamReader != null) {
        inputStreamReader.close();
      }
    }
  }

  public static Executor immediateExecutor() {
    return new Executor() {
      @Override public void execute(Runnable command) {
        command.run();
      }
    };
  }

  public static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(Utils.class, "/" + fileName), 32);
  }

  public static <T> void assertResponse(ApolloCall<T> call, Predicate<Response<T>> predicate) {
    Rx2Apollo.from(call)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertValue(predicate);
  }

  public static <T> void enqueueAndAssertResponse(MockWebServer server, String mockResponse, ApolloCall<T> call,
      Predicate<Response<T>> predicate) throws Exception {
    server.enqueue(mockResponse(mockResponse));
    assertResponse(call, predicate);
  }

  public static <T> void cacheAndAssertCachedResponse(MockWebServer server, String mockResponse, ApolloQueryCall<T> call,
      Predicate<Response<T>> predicate) throws Exception {
    server.enqueue(mockResponse(mockResponse));
    assertResponse(
        call.responseFetcher(NETWORK_ONLY),
        new Predicate<Response<T>>() {
          @Override public boolean test(Response<T> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertResponse(
        call.clone().responseFetcher(CACHE_ONLY),
        predicate
    );
  }
}
