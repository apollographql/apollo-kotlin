package com.apollographql.apollo.internal;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.cache.http.HttpCacheControl;
import com.apollographql.apollo.cache.normalized.CacheControl;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

public class CacheControlTest {

  private OkHttpClient okHttpClient;

  @Before public void setUp() {
    okHttpClient = new OkHttpClient.Builder().build();
  }

  @Test public void setDefaultCacheControl() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(okHttpClient)
        .defaultHttpCacheControl(HttpCacheControl.CACHE_ONLY)
        .defaultCacheControl(CacheControl.NETWORK_ONLY)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.newCall(
        new Operation<Operation.Data, Object, Operation.Variables>() {
          @Override public String queryDocument() {
            return null;
          }

          @Override public Variables variables() {
            return null;
          }

          @Override public ResponseFieldMapper<Data> responseFieldMapper() {
            return new ResponseFieldMapper<Data>() {
              @Override public Data map(ResponseReader responseReader) throws IOException {
                return null;
              }
            };
          }

          @Override public Object wrapData(Data data) {
            return null;
          }
        });

    assertThat(realApolloCall.httpCacheControl).isEqualTo(HttpCacheControl.CACHE_ONLY);
    assertThat(realApolloCall.cacheControl).isEqualTo(CacheControl.NETWORK_ONLY);
  }

  @Test public void defaultCacheControl() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl("http://google.com")
        .okHttpClient(okHttpClient)
        .build();

    RealApolloCall realApolloCall = (RealApolloCall) apolloClient.newCall(
        new Operation<Operation.Data, Object, Operation.Variables>() {
          @Override public String queryDocument() {
            return null;
          }

          @Override public Variables variables() {
            return null;
          }

          @Override public ResponseFieldMapper<Data> responseFieldMapper() {
            return new ResponseFieldMapper<Data>() {
              @Override public Data map(ResponseReader responseReader) throws IOException {
                return null;
              }
            };
          }

          @Override public Object wrapData(Data data) {
            return null;
          }
        });

    assertThat(realApolloCall.httpCacheControl).isEqualTo(HttpCacheControl.CACHE_FIRST);
    assertThat(realApolloCall.cacheControl).isEqualTo(CacheControl.CACHE_FIRST);
  }
}
