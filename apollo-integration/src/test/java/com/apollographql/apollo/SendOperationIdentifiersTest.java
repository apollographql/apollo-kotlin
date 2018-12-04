package com.apollographql.apollo;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.functions.Predicate;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.google.common.truth.Truth.assertThat;

public class SendOperationIdentifiersTest {
  @Rule public final MockWebServer server = new MockWebServer();

  @Test public void sendOperationIdsTrue() throws Exception {
    final HeroAndFriendsNamesQuery query = new HeroAndFriendsNamesQuery(Input.fromNullable(EMPIRE));
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .enableAutoPersistedQueries(true)
        .build();
    apolloClient.query(query).enqueue(null);

    String serverRequest = server.takeRequest().getBody().readUtf8();
    assertThat(serverRequest.contains(String.format("\"sha256Hash\":\"%s\"", query.operationId()))).isTrue();
    assertThat(serverRequest.contains("\"query\":")).isFalse();
  }

  @Test public void doesNotSendOperationIdsWhenFalse() throws Exception {
    final HeroAndFriendsNamesQuery query = new HeroAndFriendsNamesQuery(Input.fromNullable(EMPIRE));
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .enableAutoPersistedQueries(false)
        .build();
    apolloClient.query(query).enqueue(null);

    String serverRequest = server.takeRequest().getBody().readUtf8();
    assertThat(serverRequest.contains("\"id\":\"")).isFalse();
    assertThat(serverRequest.contains("\"query\":")).isTrue();
  }

  @Test public void operationIdHttpRequestHeader() throws Exception {
    final HeroAndFriendsNamesQuery heroAndFriendsNamesQuery = new HeroAndFriendsNamesQuery(Input.fromNullable(EMPIRE));
    final AtomicBoolean applicationInterceptorHeader = new AtomicBoolean();
    final AtomicBoolean networkInterceptorHeader = new AtomicBoolean();
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .dispatcher(new Dispatcher(Utils.immediateExecutorService()))
        .addInterceptor(new Interceptor() {
          @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (request.header("X-APOLLO-OPERATION-ID").equals(heroAndFriendsNamesQuery.operationId())) {
              applicationInterceptorHeader.set(true);
            }
            return chain.proceed(chain.request());
          }
        })
        .addNetworkInterceptor(new Interceptor() {
          @Override public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            if (request.header("X-APOLLO-OPERATION-ID").equals(heroAndFriendsNamesQuery.operationId())) {
              networkInterceptorHeader.set(true);
            }
            return chain.proceed(chain.request());
          }
        })
        .build();

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(Utils.immediateExecutor())
        .build();

    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(heroAndFriendsNamesQuery),
        new Predicate<com.apollographql.apollo.api.Response<HeroAndFriendsNamesQuery.Data>>() {
          @Override
          public boolean test(com.apollographql.apollo.api.Response<HeroAndFriendsNamesQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );

    assertThat(applicationInterceptorHeader.get()).isTrue();
    assertThat(networkInterceptorHeader.get()).isTrue();
  }
}
