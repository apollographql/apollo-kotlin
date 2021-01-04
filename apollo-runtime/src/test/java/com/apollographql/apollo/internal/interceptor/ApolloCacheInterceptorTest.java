package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.Logger;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ApolloCacheInterceptorTest {
  private ApolloCacheInterceptor interceptor;
  private ApolloStore apolloStore;
  private Logger logger;
  private okhttp3.Response okHttpResponse;

  @Rule public final MockWebServer server = new MockWebServer();

  @Before
  public void setUp() {
    apolloStore = mock(ApolloStore.class);
    logger = mock(Logger.class);
    okHttpResponse = new okhttp3.Response.Builder()
        .request(new Request.Builder().url(server.url("/")).build())
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build();

    interceptor = new ApolloCacheInterceptor(
        apolloStore,
        mock(Executor.class),
        new ApolloLogger(logger),
        false
    );
  }

  @Test
  public void testDoesNotCacheErrorResponse() {
    Operation<?,?> operation = mock(Operation.class);
    Error error = new Error("Error", Collections.emptyList(), Collections.emptyMap());
    ApolloInterceptor.InterceptorResponse networkResponse = new ApolloInterceptor.InterceptorResponse(
        okHttpResponse,
        com.apollographql.apollo.api.Response.builder(operation).errors(Collections.singletonList(error)).build(),
        new ArrayList<Record>()
    );
    ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(operation).build();

    Set<String> cachedKeys = interceptor.cacheResponse(networkResponse, request);

    assertThat(cachedKeys).isEmpty();
    verifyZeroInteractions(apolloStore, logger);
  }

  @Test
  public void testCachesErrorResponseWhenStorePartialResponsesCacheHeaderPresent() {
    Operation<?> operation = mock(Operation.class);
    Error error = new Error("Error", Collections.emptyList(), Collections.emptyMap());
    ApolloInterceptor.InterceptorResponse networkResponse = new ApolloInterceptor.InterceptorResponse(
        okHttpResponse,
        com.apollographql.apollo.api.Response.builder(operation).errors(Collections.singletonList(error)).build(),
        new ArrayList<Record>()
    );
    ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(operation).cacheHeaders(
        CacheHeaders.builder()
            .addHeader(ApolloCacheHeaders.STORE_PARTIAL_RESPONSES, "true")
            .build()
    ).build();
    Set<String> expectedCachedKeys = Collections.singleton("cacheKey");

    when(apolloStore.writeTransaction(any())).thenReturn(expectedCachedKeys);

    Set<String> cachedKeys = interceptor.cacheResponse(networkResponse, request);

    assertThat(cachedKeys).isEqualTo(expectedCachedKeys);
    verify(apolloStore).writeTransaction(any());
    verifyZeroInteractions(logger);
  }

  @Test
  public void testDoesCachesNonErrorResponse() {
    Operation<?> operation = mock(Operation.class);
    ApolloInterceptor.InterceptorResponse networkResponse = new ApolloInterceptor.InterceptorResponse(
        okHttpResponse,
        com.apollographql.apollo.api.Response.builder(operation).build(),
        new ArrayList<Record>()
    );
    ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(operation).build();
    Set<String> expectedCachedKeys = Collections.singleton("cacheKey");

    when(apolloStore.writeTransaction(any())).thenReturn(expectedCachedKeys);

    Set<String> cachedKeys = interceptor.cacheResponse(networkResponse, request);

    assertThat(cachedKeys).isEqualTo(expectedCachedKeys);
    verify(apolloStore).writeTransaction(any());
    verifyZeroInteractions(logger);
  }
}
