package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.mpp.currentThreadName
import com.apollographql.apollo3.testing.QueueTestNetworkTransport
import com.apollographql.apollo3.testing.enqueueTestResponse
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class NormalizedCacheThreadingTest {
  @Test
  fun cacheCreationHappensInBackgroundThread() = runTest {
    val testThreadName = currentThreadName()
    // No threading on js
    if (testThreadName == "js") return@runTest
    var cacheCreateThreadName: String? = null
    val apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .normalizedCache(object : NormalizedCacheFactory() {
          override fun create(): NormalizedCache {
            cacheCreateThreadName = currentThreadName()
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
