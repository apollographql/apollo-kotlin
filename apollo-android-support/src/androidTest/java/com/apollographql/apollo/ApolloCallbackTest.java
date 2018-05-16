package com.apollographql.apollo;

import android.os.Handler;
import android.os.Message;
import android.support.test.runner.AndroidJUnit4;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.TestUtils.EMPTY_QUERY;
import static com.apollographql.apollo.TestUtils.createBackgroundLooper;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class ApolloCallbackTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloClient apolloClient;
  private static final int TIMEOUT_SECONDS = 2;

  @Before public void setUp() throws Exception {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build())
        .build();
  }

  @Test public void onHttpError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    final AtomicReference<ApolloException> exceptionRef = new AtomicReference<>();
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    apolloClient.query(EMPTY_QUERY).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@NotNull Response response) {
        countDownLatch.countDown();
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        exceptionRef.set(e);
        countDownLatch.countDown();
      }

      @Override public void onHttpError(@NotNull ApolloHttpException e) {
        exceptionRef.set(e);
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
    assertThat(exceptionRef.get()).isInstanceOf(ApolloHttpException.class);
  }

  @Test public void onNetworkError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    final AtomicReference<ApolloException> exceptionRef = new AtomicReference<>();
    apolloClient.query(EMPTY_QUERY).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@NotNull Response response) {
        countDownLatch.countDown();
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        countDownLatch.countDown();
      }

      @Override public void onNetworkError(@NotNull ApolloNetworkException e) {
        exceptionRef.set(e);
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
    assertThat(exceptionRef.get()).isInstanceOf(ApolloNetworkException.class);
  }

  @Test public void onParseError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    final AtomicReference<ApolloException> exceptionRef = new AtomicReference<>();
    server.enqueue(new MockResponse().setResponseCode(200).setBody("nonsense"));
    apolloClient.query(EMPTY_QUERY).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@NotNull Response response) {
        countDownLatch.countDown();
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        countDownLatch.countDown();
      }

      @Override public void onParseError(@NotNull ApolloParseException e) {
        exceptionRef.set(e);
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
    assertThat(exceptionRef.get()).isInstanceOf(ApolloParseException.class);
  }

  @Test public void onResponse() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    final AtomicReference<Response> responseRef = new AtomicReference<>();
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{" +
        "  \"errors\": [" +
        "    {" +
        "      \"message\": \"Cannot query field \\\"names\\\" on type \\\"Species\\\".\"," +
        "      \"locations\": [" +
        "        {" +
        "          \"line\": 3," +
        "          \"column\": 5" +
        "        }" +
        "      ]" +
        "    }" +
        "  ]" +
        "}"));
    apolloClient.query(EMPTY_QUERY).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@NotNull Response response) {
        responseRef.set(response);
        countDownLatch.countDown();
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
    assertThat(responseRef.get()).isNotNull();
  }

  private static Handler mockCallbackHandler(final AtomicBoolean invokeTracker) throws Exception {
    return new Handler(createBackgroundLooper()) {
      @Override public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        invokeTracker.set(true);
        msg.getCallback().run();
        return true;
      }
    };
  }
}
