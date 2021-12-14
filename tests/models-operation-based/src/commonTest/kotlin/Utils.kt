import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.testing.pathToJsonReader
import com.apollographql.apollo3.testing.pathToUtf8

fun testFixtureToUtf8(name: String) = pathToUtf8("models-fixtures/json/$name")
fun testFixtureToJsonReader(name: String) = pathToJsonReader("models-fixtures/json/$name")

/**
 * A [CacheKeyGenerator] that always uses the "id" field if it exists and delegates to [TypePolicyCacheKeyGenerator] else
 *
 * It will coerce Int, Floats and other types to String using [toString]
 */
object IdCacheKeyGenerator : CacheKeyGenerator {
  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
    return obj["id"]?.toString()?.let { CacheKey(it) } ?: TypePolicyCacheKeyGenerator.cacheKeyForObject(obj, context)
  }
}