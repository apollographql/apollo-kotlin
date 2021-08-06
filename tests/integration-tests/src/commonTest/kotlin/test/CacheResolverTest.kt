package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.DefaultCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.cache.normalized.withStore
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CacheResolverTest {
  @Test
  fun cacheResolverCanResolveQuery() = runTest {
    val resolver = object : CacheResolver {
      override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
        return when (field.name) {
          "hero" -> mapOf("name" to "Luke")
          else -> DefaultCacheResolver.resolveField(field, variables, parent, parentId)
        }
      }
    }
    val apolloClient = ApolloClient(serverUrl = "").withStore(
        ApolloStore(
            normalizedCacheFactory = MemoryCacheFactory(),
            cacheResolver = resolver
        )
    )

    val response = apolloClient.query(HeroNameQuery())

    assertEquals("Luke", response.data?.hero?.name)
  }
}