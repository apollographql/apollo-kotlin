package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.internal.cache.http.HttpCacheFetchStrategy;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

public class CacheControlTest {
  private OkHttpClient okHttpClient;
  private Query emptyQuery;

  @Before public void setUp() {
    okHttpClient = new OkHttpClient.Builder().build();
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

      @Nonnull @Override public OperationName name() {
        return null;
      }

      @Override public Object wrapData(Data data) {
        return data;
      }
    };
  }

  @Test public void setDefaultCachePolicy() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(okHttpClient)
        .defaultHttpCachePolicy(HttpCachePolicy.CACHE_ONLY)
        .defaultCacheControl(CacheControl.NETWORK_ONLY)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.query(emptyQuery);
    assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(HttpCacheFetchStrategy.CACHE_ONLY);
    assertThat(realApolloCall.cacheControl).isEqualTo(CacheControl.NETWORK_ONLY);
  }

  @Test public void defaultCacheControl() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(okHttpClient)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.query(emptyQuery);
    assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(HttpCacheFetchStrategy.NETWORK_ONLY);
    assertThat(realApolloCall.cacheControl).isEqualTo(CacheControl.CACHE_FIRST);
  }
}
