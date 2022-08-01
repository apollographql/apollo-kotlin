package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSThread
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeThreadTest {
  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun canExecuteOperationsFromAnyThread() {
    val context = newSingleThreadContext("test")
    try {
      runBlocking(context = context) {
        assertTrue(!NSThread.isMainThread)

        val mockServer = MockServer()
        mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

        val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
        val response = apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).execute()
        mockServer.stop()
        assertEquals(response.data?.hero?.name, "R2-D2")
      }
    } finally {
      context.close()
    }
  }
}