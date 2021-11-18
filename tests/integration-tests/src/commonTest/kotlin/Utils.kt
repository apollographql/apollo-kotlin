import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.testing.checkFile
import com.apollographql.apollo3.testing.readFile
import kotlin.test.assertEquals

fun readTestFixture(name: String) = readFile("testFixtures/$name")
fun checkTestFixture(actualText: String, name: String) = checkFile(actualText, "testFixtures/$name")

fun readResource(name: String) = readFile("testFixtures/resources/$name")


/**
 * A helper function to reverse the order of the argument so that we can easily column edit the tests
 */
fun assertEquals2(actual: Any?, expected: Any?) = assertEquals(expected, actual)


/**
 * A [CacheResolver] that looks for an "id" argument to resolve fields and delegates to [FieldPolicyCacheResolver] else
 */
object IdCacheResolver: CacheResolver {
  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
    val id = field.resolveArgument("id", variables)?.toString()
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