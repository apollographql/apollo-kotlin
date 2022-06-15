package test.embed

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.normalize
import embed.GetHeroQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class EmbedTest {
  @Test
  fun test() {
    val query = GetHeroQuery()

    val records = query.normalize(
        data = GetHeroQuery.Data(
            hero = GetHeroQuery.Hero(
                friends = listOf(GetHeroQuery.Friend("Luke"), GetHeroQuery.Friend("Leia"))
            )
        ),
        customScalarAdapters = CustomScalarAdapters.Empty,
        cacheKeyGenerator = object : CacheKeyGenerator {
          override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
            return null
          }
        }
    )

    assertEquals(1, records.size)
  }
}
