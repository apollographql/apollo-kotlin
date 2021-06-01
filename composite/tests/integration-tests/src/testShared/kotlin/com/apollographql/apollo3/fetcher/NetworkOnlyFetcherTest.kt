package com.apollographql.apollo3.fetcher

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.isFromCache
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import java.net.HttpURLConnection

class NetworkOnlyFetcherTest : BaseFetcherTest() {
  @Test
  fun enqueue() {
    val query = EpisodeHeroNameQuery(episode = Episode.EMPIRE)
    var trackingCallback: TrackingCallback

    // Has error when cache empty, and network error
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server Error"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions.size).isEqualTo(1)

    // Goes to network when empty
    server.enqueue(mockResponse("HeroNameResponse.json"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions).isEmpty()
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isFalse()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")

    // Goes to network after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions).isEmpty()
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isFalse()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")

    // Has error when cache empty, and network error
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR).setBody("Server Error"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions.size).isEqualTo(1)
  }
}