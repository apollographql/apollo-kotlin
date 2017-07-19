package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.internal.cache.http.HttpCacheFetchStrategy;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;

import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.CACHE_FIRST;
import static com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY;
import static com.google.common.truth.Truth.assertThat;

public class ResponseFetcherTest {
  private OkHttpClient okHttpClient;
  private Query emptyQuery;

  @Before public void setUp() {
    okHttpClient = new OkHttpClient.Builder().build();

    emptyQuery = new Query() {
      OperationName operationName = new OperationName() {
        @Override public String name() {
          return "emptyQuery";
        }
      };
      @Override public String queryDocument() {
        return "";
      }

      @Override public Variables variables() {
        return EMPTY_VARIABLES;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return new ResponseFieldMapper<Data>() {
          @Override public Data map(ResponseReader responseReader) {
            return null;
          }
        };
      }

      @Nonnull @Override public OperationName name() {
        return operationName;
      }

      @Nonnull @Override public String operationId() {
        return "";
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
        .defaultResponseFetcher(NETWORK_ONLY)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.query(emptyQuery);
    assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(HttpCacheFetchStrategy.CACHE_ONLY);
    assertThat(realApolloCall.responseFetcher).isEqualTo(NETWORK_ONLY);
  }

  @Test public void defaultCacheControl() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(okHttpClient)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.query(emptyQuery);
    assertThat(realApolloCall.httpCachePolicy.fetchStrategy).isEqualTo(HttpCacheFetchStrategy.NETWORK_ONLY);
    assertThat(realApolloCall.responseFetcher).isEqualTo(CACHE_FIRST);
  }
}
