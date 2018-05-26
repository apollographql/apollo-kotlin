package com.apollographql.apollo.internal.interceptor;

import com.google.common.base.Predicate;

import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.integration.interceptor.AllFilmsQuery;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.junit.Test;

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
  private final String expectedRequestBody = "{\"query\":\"query AllFilms($after: String, $first: Int, $before: String, $last: Int) {  " +
      "allFilms(after: $after, first: $first, before: $before, last: $last) {    " +
      "__typename    totalCount    films {      __typename      title      releaseDate    }  }}\"," +
      "\"operationName\":\"AllFilms\",\"variables\":{\"after\":\"some cursor\",\"first\":null,\"last\":100}}";

  @Test public void testDefaultHttpCall() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
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
        new ApolloLogger(Optional.<Logger>absent()), false);

    interceptor.httpCall(query);
  }

  @Test public void testCachedHttpCall() throws Exception {
    Predicate<Request> requestAssertPredicate = new Predicate<Request>() {
      @Override public boolean apply(@Nullable Request request) {
        assertThat(request).isNotNull();
        assertDefaultRequestHeaders(request);
        assertThat(request.header(HttpCache.CACHE_KEY_HEADER)).isEqualTo("a76097787b9ec95cd90eb8b53c6711ff");
        assertThat(request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)).isEqualTo("NETWORK_FIRST");
        assertThat(request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)).isEqualTo("10000");
        assertThat(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER)).isEqualTo("false");
        assertThat(request.header(HttpCache.CACHE_PREFETCH_HEADER)).isEqualTo("false");
        assertRequestBody(request);
        return true;
      }
    };

    ApolloServerInterceptor interceptor = new ApolloServerInterceptor(serverUrl,
        new AssertHttpCallFactory(requestAssertPredicate),
        HttpCachePolicy.NETWORK_FIRST.expireAfter(10, TimeUnit.SECONDS),
        false, new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        new ApolloLogger(Optional.<Logger>absent()), false);

    interceptor.httpCall(query);
  }

  private void assertDefaultRequestHeaders(Request request) {
    assertThat(request.url()).isEqualTo(serverUrl);
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.header(ApolloServerInterceptor.HEADER_ACCEPT_TYPE)).isEqualTo(ApolloServerInterceptor.ACCEPT_TYPE);
    assertThat(request.header(ApolloServerInterceptor.HEADER_CONTENT_TYPE)).isEqualTo(ApolloServerInterceptor.CONTENT_TYPE);
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
    assertThat(bodyBuffer.readUtf8()).isEqualTo(expectedRequestBody);
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

    @Override public Response execute() throws IOException {
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
  }
}
