package com.example

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Test

internal object IdBasedCacheKeyResolver : CacheResolver, CacheKeyGenerator {

  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext) =
      obj["id"]?.toString()?.let(::CacheKey) ?: TypePolicyCacheKeyGenerator.cacheKeyForObject(obj, context)

  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String) =
      FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
}

class NormalizationTest() {

  @Test
  fun issue3672() = runBlocking {
    val store = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        cacheKeyGenerator = IdBasedCacheKeyResolver,
        cacheResolver = IdBasedCacheKeyResolver
    )

    val query = Issue3672Query()

    val data1 = query.parseJsonResponse(Buffer().writeUtf8(nestedResponse).jsonReader(), CustomScalarAdapters.Empty).dataAssertNoErrors
    store.writeOperation(query, data1)

    val data2 =  store.readOperation(query)
    check(data1 == data2)
  }
}
