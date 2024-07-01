import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.testing.pathToJsonReader
import com.apollographql.apollo.testing.pathToUtf8


@Suppress("DEPRECATION")
fun testFixtureToUtf8(name: String) = pathToUtf8("models-fixtures/json/$name")
@Suppress("DEPRECATION")
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