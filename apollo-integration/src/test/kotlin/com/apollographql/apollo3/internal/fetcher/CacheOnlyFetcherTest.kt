package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeoutException

class CacheOnlyFetcherTest : BaseFetcherTest() {
  @Test
  @Throws(IOException::class, ApolloException::class, TimeoutException::class, InterruptedException::class)
  fun enqueue() {
    val query = EpisodeHeroNameQuery(episode = Input.present(Episode.EMPIRE))
    var trackingCallback: TrackingCallback

    // Is null when cache empty
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions.size).isEqualTo(0)
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isTrue()
    Truth.assertThat(trackingCallback.responseList[0].data).isNull()
    Truth.assertThat(server.requestCount).isEqualTo(0)

    // Populate cache
    server.enqueue(mockResponse("HeroNameResponse.json"))
    val responseData = Rx2Apollo.from(apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)).blockingFirst()
    Truth.assertThat(responseData.hasErrors()).isFalse()
    assertThat(responseData.data!!.hero?.name).isEqualTo("R2-D2")
    Truth.assertThat(server.requestCount).isEqualTo(1)

    // Success after cache populated
    server.enqueue(mockResponse("HeroNameResponse.json"))
    trackingCallback = TrackingCallback()
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).enqueue(trackingCallback)
    Truth.assertThat(trackingCallback.exceptions.size).isEqualTo(0)
    Truth.assertThat(trackingCallback.responseList.size).isEqualTo(1)
    Truth.assertThat(trackingCallback.responseList[0].isFromCache).isTrue()
    assertThat(trackingCallback.responseList[0].data!!.hero?.name).isEqualTo("R2-D2")
    Truth.assertThat(server.requestCount).isEqualTo(1)
  }
}