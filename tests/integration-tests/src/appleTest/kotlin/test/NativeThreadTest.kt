package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.DelicateCoroutinesApi
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
  @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
  fun canExecuteOperationsFromAnyThread() {
    val context = newSingleThreadContext("test")
    try {
      runBlocking(context = context) {
        assertTrue(!NSThread.isMainThread)

        val mockServer = MockServer()
        mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))

        val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
        val response = apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE)).execute()
        mockServer.close()
        assertEquals(response.data?.hero?.name, "R2-D2")
      }
    } finally {
      context.close()
    }
  }
}