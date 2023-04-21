package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.testing.internal.runTest
import embed.GetHeroQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class EmbedTest {
  @Test
  fun normalize() {
    val query = GetHeroQuery()

    val records = query.normalize(
        GetHeroQuery.Data(
            GetHeroQuery.Hero(
                listOf(GetHeroQuery.Friend("Luke", GetHeroQuery.Pet("Snoopy")), GetHeroQuery.Friend("Leia", GetHeroQuery.Pet("Fluffy")))
            )
        ),
        CustomScalarAdapters.Empty,
        object : CacheKeyGenerator {
          override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
            return null
          }
        }
    )

    assertEquals(3, records.size)
  }

  @Test
  fun denormalize() = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(normalizedCacheFactory = MemoryCacheFactory())
        .serverUrl("unused")
        .build()

    val query = GetHeroQuery()
    val data = GetHeroQuery.Data(
        GetHeroQuery.Hero(
            listOf(GetHeroQuery.Friend("Luke", GetHeroQuery.Pet("Snoopy")), GetHeroQuery.Friend("Leia", GetHeroQuery.Pet("Fluffy")))
        )
    )
    client.apolloStore.writeOperation(query, data)
    val dataFromStore = client.apolloStore.readOperation(query)
    assertEquals(data, dataFromStore)
  }
}
