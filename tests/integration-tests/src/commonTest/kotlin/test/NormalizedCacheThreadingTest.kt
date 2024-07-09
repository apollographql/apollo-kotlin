package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo.testing.QueueTestNetworkTransport
import com.apollographql.apollo.testing.currentThreadId
import com.apollographql.apollo.testing.enqueueTestResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class NormalizedCacheThreadingTest {
  @Test
  fun cacheCreationHappensInBackgroundThread() = runTest {
    @Suppress("DEPRECATION")
    val testThreadName = currentThreadId()
    // No threading on js
    if (testThreadName == "js") return@runTest
    var cacheCreateThreadName: String? = null
    val apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .normalizedCache(object : NormalizedCacheFactory() {
          override fun create(): NormalizedCache {
            @Suppress("DEPRECATION")
            cacheCreateThreadName = currentThreadId()
            return MemoryCacheFactory().create()
          }
        }).build()
    assertNull(cacheCreateThreadName)

    val query = CharacterNameByIdQuery("")
    apolloClient.enqueueTestResponse(query, CharacterNameByIdQuery.Data(CharacterNameByIdQuery.Character("")))
    apolloClient.query(query).execute()
    println("cacheCreateThreadName: $cacheCreateThreadName")
    assertNotEquals(testThreadName, cacheCreateThreadName)
  }
}
