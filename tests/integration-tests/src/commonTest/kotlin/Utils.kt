import IdCacheKeyGenerator.toString
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo.cache.normalized.api.CacheResolver
import com.apollographql.apollo.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.testing.checkFile
import com.apollographql.apollo.testing.pathToJsonReader
import com.apollographql.apollo.testing.pathToUtf8
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
fun checkTestFixture(actualText: String, name: String) = checkFile(actualText, "integration-tests/testFixtures/$name")
@Suppress("DEPRECATION")
fun testFixtureToUtf8(name: String) = pathToUtf8("integration-tests/testFixtures/$name")
@Suppress("DEPRECATION")
fun testFixtureToJsonReader(name: String) = pathToJsonReader("integration-tests/testFixtures/$name")

/**
 * A helper function to reverse the order of the argument so that we can easily column edit the tests
 */
fun assertEquals2(actual: Any?, expected: Any?) = assertEquals(expected, actual)

/**
 * A [CacheResolver] that looks for an "id" argument to resolve fields and delegates to [FieldPolicyCacheResolver] else
 */
object IdCacheResolver: CacheResolver {
  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
    val id = field.argumentValue("id", variables).getOrNull()?.toString()
    if (id != null) {
      return CacheKey(id)
    }

    return FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
  }
}

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