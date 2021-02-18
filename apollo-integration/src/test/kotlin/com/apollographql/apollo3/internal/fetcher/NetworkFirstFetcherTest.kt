package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeoutException

class NetworkFirstFetcherTest : BaseFetcherTest() {
  @Test
  @Throws(IOException::class, ApolloException::class, TimeoutException::class, InterruptedException::class)
  fun enqueue() {
    val query = EpisodeHeroNameQuery(episode = Input.present(Episode.EMPIRE))
    var trackingCallback: TrackingCallback

    // Has error when cache empty, and network error
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server Error"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions.size).isEqualTo(1)

    // Goes to network when empty
    server.enqueue(mockResponse("HeroNameResponse.json"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions).isEmpty()
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isFalse()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")

    // Goes to network after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions).isEmpty()
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isFalse()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")

    // Falls back to cache if network error
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server Error"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions).isEmpty()
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isTrue()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")
  }
}