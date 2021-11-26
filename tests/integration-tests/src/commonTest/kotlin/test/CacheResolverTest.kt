package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.DefaultCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
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
    val apolloClient = ApolloClient.Builder().serverUrl(serverUrl = "")
        .store(
            ApolloStore(
                normalizedCacheFactory = MemoryCacheFactory(),
                cacheResolver = resolver
            )
        )
        .build()

    val response = apolloClient.query(HeroNameQuery()).execute()

    assertEquals("Luke", response.data?.hero?.name)
  }
}