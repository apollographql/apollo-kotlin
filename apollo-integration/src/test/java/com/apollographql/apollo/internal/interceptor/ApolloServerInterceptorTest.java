package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.Utils;
import com.google.common.base.Predicate;

import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.integration.interceptor.AllFilmsQuery;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.request.RequestHeaders;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.Timeout;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class ApolloServerInterceptorTest {
  private final HttpUrl serverUrl = HttpUrl.parse("http://google.com");
  private final AllFilmsQuery query = AllFilmsQuery.builder()
      .after("some cursor")
      .beforeInput(Input.<String>absent())
      .firstInput(Input.<Integer>fromNullable(null))
      .last(100)
      .build();

  @Test public void testDefaultHttpCall() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
        assertThat(request.url()).isEqualTo(serverUrl);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertRequestBody(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()));

    interceptor.httpPostCall(query, CacheHeaders.NONE, RequestHeaders.NONE, true, false);
  }

  @Test public void testCachedHttpCall() throws Exception {
    ScalarTypeAdapters scalarTypeAdapters =
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap());
    final String cacheKey = ApolloServerInterceptor.cacheKey(query, scalarTypeAdapters);
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
        assertThat(request.url()).isEqualTo(serverUrl);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isEqualTo(cacheKey);
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isEqualTo("NETWORK_FIRST");
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isEqualTo("10000");
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isEqualTo("false");
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isEqualTo("false");
        assertThat(request.header(HttpCache.CACHE_DO_NOT_STORE)).isEqualTo("true");
        assertRequestBody(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate),
        HttpCachePolicy.NETWORK_FIRST.expireAfter(10, TimeUnit.SECONDS), false,
        scalarTypeAdapters, new ApolloLogger(Optional.<Logger>absent()));

    interceptor.httpPostCall(query, CacheHeaders.builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build(),
        RequestHeaders.NONE, true, false);
  }

  @Test public void testAdditionalHeaders() throws Exception {
    final String testHeader1 = "TEST_HEADER_1";
    final String testHeaderValue1 = "crappy_value";
    final String testHeader2 = "TEST_HEADER_2";
    final String testHeaderValue2 = "fantastic_value";
    final String testHeader3 = "TEST_HEADER_3";
    final String testHeaderValue3 = "awesome_value";

    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
        assertThat(request.url()).isEqualTo(serverUrl);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertThat(request.header(testHeader1)).isEqualTo(testHeaderValue1);
        assertThat(request.header(testHeader2)).isEqualTo(testHeaderValue2);
        assertThat(request.header(testHeader3)).isEqualTo(testHeaderValue3);
        assertRequestBody(request);
        return true;
      }
    };

    RequestHeaders requestHeaders = RequestHeaders.builder()
        .addHeader(testHeader1, testHeaderValue1)
        .addHeader(testHeader2, testHeaderValue2)
        .addHeader(testHeader3, testHeaderValue3)
        .build();

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()));

    interceptor.httpPostCall(query, CacheHeaders.NONE, requestHeaders, true, false);
  }

  @Test public void testUseHttpGetForQueries() throws IOException {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isNull();
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isNull();
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isNull();
        assertThat(request.url().queryParameter("query")).isEqualTo(query.queryDocument().replace("\n", ""));
        assertThat(request.url().queryParameter("operationName")).isEqualTo(query.name().name());
        assertThat(request.url().queryParameter("variables")).isEqualTo("{\"after\":\"some cursor\",\"first\":null,\"last\":100}");
        assertThat(request.url().queryParameter("extensions")).isEqualTo("{\"persistedQuery\":{\"version\":1," +
            "\"sha256Hash\":\"" + query.operationId() + "\"}}");
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate), null, false,
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()));

    interceptor.httpGetCall(query, CacheHeaders.NONE, RequestHeaders.NONE, true, true);
  }

  private void assertDefaultRequestHeaders(Request request) {
    assertThat(request.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.ACCEPT_TYPE);
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_ID)).isEqualTo(query.operationId());
    assertThat(request.header(ApolloServerInterceptor.HEADER_APOLLO_OPERATION_NAME)).isEqualTo(query.name().name());
    assertThat(request.tag()).isEqualTo(query.operationId());
  }

  private void assertRequestBody(Request request) {
    assertThat(request.body().contentType()).isEqualTo(ApolloServerInterceptor.MEDIA_TYPE);
    Buffer bodyBuffer = new Buffer();
    try {
      request.body().writeTo(bodyBuffer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Utils.INSTANCE.checkTestFixture(bodyBuffer.readUtf8(), "ApolloServerInterceptorTest/interceptorRequestBody.json");
  }

  private static class AssertHttpCallFactory implements Call.Factory {
    final Predicate<Request> predicate;

    AssertHttpCallFactory(Predicate<Request> predicate) {
      this.predicate = predicate;
    }

    @Override public Call newCall(@NotNull Request request) {
      if (!predicate.apply(request)) {
        fail("Assertion failed");
      }
      return new NoOpCall();
    }
  }

  private static class NoOpCall implements Call {
    @Override public Request request() {
      return null;
    }

    @Override public Response execute() {
      return null;
    }

    @Override public void enqueue(Callback responseCallback) {
    }

    @Override public void cancel() {
    }

    @Override public boolean isExecuted() {
      return false;
    }

    @Override public boolean isCanceled() {
      return false;
    }

    @Override public Call clone() {
      return null;
    }

    @Override public Timeout timeout() {
      return null;
    }
  }
}
