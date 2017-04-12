package com.apollographql.apollo.cache.normalized.sql;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.test.runner.AndroidJUnit4;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloCallback;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
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

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class ApolloCallbackTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloClient apolloClient;
  private Query emptyQuery;

  @Before public void setUp() throws Exception {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build())
        .build();

    emptyQuery = new Query() {
      @Override public String queryDocument() {
        return "";
      }

      @Override public Variables variables() {
        return EMPTY_VARIABLES;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return new ResponseFieldMapper<Data>() {
          @Override public Data map(ResponseReader responseReader) throws IOException {
            return null;
          }
        };
      }

      @Override public Object wrapData(Data data) {
        return data;
      }
    };
  }

  @Test public void onHttpError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    apolloClient.newCall(emptyQuery).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected onHttpError");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected onHttpError");
      }

      @Override public void onHttpError(@Nonnull ApolloHttpException e) {
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(20, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
  }

  @Test public void onNetworkError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    apolloClient.newCall(emptyQuery).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected onNetworkError");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected onNetworkError");
      }

      @Override public void onNetworkError(@Nonnull ApolloNetworkException e) {
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(4, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
  }

  @Test public void onParseError() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
    Query query = new Query() {
      @Override public String queryDocument() {
        return "";
      }

      @Override public Variables variables() {
        return EMPTY_VARIABLES;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return new ResponseFieldMapper<Data>() {
          @Override public Data map(ResponseReader responseReader) throws IOException {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override public Object wrapData(Data data) {
        throw new UnsupportedOperationException();
      }
    };

    server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"data\": {}}"));
    apolloClient.newCall(query).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected onParseError");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected onParseError");
      }

      @Override public void onParseError(@Nonnull ApolloParseException e) {
        countDownLatch.countDown();
      }
    }, callbackHandler));
    countDownLatch.await(2, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
  }

  @Test public void onResponse() throws Exception {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicBoolean invoked = new AtomicBoolean();
    final Handler callbackHandler = mockCallbackHandler(invoked);
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
    apolloClient.newCall(emptyQuery).enqueue(ApolloCallback.wrap(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        countDownLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected onResponse");
      }
    }, callbackHandler));
    countDownLatch.await(2, TimeUnit.SECONDS);
    assertThat(invoked.get()).isTrue();
  }

  private static Handler mockCallbackHandler(final AtomicBoolean invokeTracker) throws Exception {
    final AtomicReference<Looper> looperRef = new AtomicReference<>();
    new Thread() {
      @Override public void run() {
        Looper.prepare();
        synchronized (this) {
          looperRef.set(Looper.myLooper());
          notifyAll();
        }
        Looper.loop();
      }
    }.start();

    Thread.sleep(200);
    return new Handler(looperRef.get()) {
      @Override public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        invokeTracker.set(true);
        msg.getCallback().run();
        return true;
      }
    };
  }
}
