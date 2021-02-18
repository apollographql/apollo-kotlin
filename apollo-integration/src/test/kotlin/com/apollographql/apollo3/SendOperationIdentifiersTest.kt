package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class SendOperationIdentifiersTest {
  val server = MockWebServer()
  @Test
  @Throws(Exception::class)
  fun sendOperationIdsTrue() {
    val query = HeroAndFriendsNamesQuery(Input.present(Episode.EMPIRE))
    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .enableAutoPersistedQueries(true)
        .build()
    apolloClient.query(query).enqueue(null)
    val serverRequest = server.takeRequest().body.readUtf8()
    Truth.assertThat(serverRequest.contains("extensions")).isTrue()
    Truth.assertThat(serverRequest.contains("persistedQuery")).isTrue()
    Truth.assertThat(serverRequest.contains(String.format("\"sha256Hash\":\"%s\"", query.operationId()))).isTrue()
    Truth.assertThat(serverRequest.contains("\"query\":")).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun doesNotSendOperationIdsWhenFalse() {
    val query = HeroAndFriendsNamesQuery(Input.present(Episode.EMPIRE))
    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .enableAutoPersistedQueries(false)
        .build()
    apolloClient.query(query).enqueue(null)
    val serverRequest = server.takeRequest().body.readUtf8()
    Truth.assertThat(serverRequest.contains("extensions")).isFalse()
    Truth.assertThat(serverRequest.contains("persistedQuery")).isFalse()
    Truth.assertThat(serverRequest.contains("sha256Hash")).isFalse()
    Truth.assertThat(serverRequest.contains("\"query\":")).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun operationIdHttpRequestHeader() {
    val heroAndFriendsNamesQuery = HeroAndFriendsNamesQuery(Input.present(Episode.EMPIRE))
    val applicationInterceptorHeader = AtomicBoolean()
    val networkInterceptorHeader = AtomicBoolean()
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .addInterceptor { chain ->
          val request = chain.request()
          if (request.header("X-APOLLO-OPERATION-ID") == heroAndFriendsNamesQuery.operationId()) {
            applicationInterceptorHeader.set(true)
          }
          chain.proceed(chain.request())
        }
        .addNetworkInterceptor { chain ->
          val request = chain.request()
          if (request.header("X-APOLLO-OPERATION-ID") == heroAndFriendsNamesQuery.operationId()) {
            networkInterceptorHeader.set(true)
          }
          chain.proceed(chain.request())
        }
        .build()
    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(immediateExecutor())
        .build()
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient.query(heroAndFriendsNamesQuery)
    ) { response -> !response.hasErrors() }
    Truth.assertThat(applicationInterceptorHeader.get()).isTrue()
    Truth.assertThat(networkInterceptorHeader.get()).isTrue()
  }
}
