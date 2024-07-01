package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.CacheResolver
import com.apollographql.apollo.cache.normalized.api.DefaultCacheResolver
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.testing.internal.runTest
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
