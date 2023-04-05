package com.example

import com.apollographql.apollo3.ApolloClient
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
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.example.one.Issue2818Query
import com.example.one.Issue3672Query
import com.example.one.fragment.SectionFragment
import com.example.two.GetCountryQuery
import com.example.two.NestedFragmentQuery
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Test

internal object IdBasedCacheKeyResolver : CacheResolver, CacheKeyGenerator {

  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext) =
      obj["id"]?.toString()?.let(::CacheKey) ?: TypePolicyCacheKeyGenerator.cacheKeyForObject(obj, context)

  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String) =
      FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
}

class NormalizationTest {

  @Test
  fun issue3672() = runBlocking {
    val store = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        cacheKeyGenerator = IdBasedCacheKeyResolver,
        cacheResolver = IdBasedCacheKeyResolver
    )

    val query = Issue3672Query()

    val data1 = query.parseJsonResponse(Buffer().writeUtf8(nestedResponse).jsonReader(), CustomScalarAdapters.Empty).dataOrThrow()
    store.writeOperation(query, data1)

    val data2 = store.readOperation(query)
    check(data1 == data2)
  }

  @Test
  fun issue3672_2() = runBlocking {
    val store = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        cacheKeyGenerator = IdBasedCacheKeyResolver,
        cacheResolver = IdBasedCacheKeyResolver
    )

    val query = NestedFragmentQuery()

    val data1 = query.parseJsonResponse(Buffer().writeUtf8(nestedResponse_list).jsonReader(), CustomScalarAdapters.Empty).dataOrThrow()
    store.writeOperation(query, data1)

    val data2 = store.readOperation(query)
    check(data1 == data2)
  }

  @Test
  fun issue2818() = runBlocking {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = MemoryCacheFactory(),
        cacheKeyGenerator = IdBasedCacheKeyResolver,
        cacheResolver = IdBasedCacheKeyResolver
    )

    apolloStore.writeOperation(
        Issue2818Query(),
        Issue2818Query.Data(
            Issue2818Query.Home(
                __typename = "Home",
                sectionA = Issue2818Query.SectionA(
                    name = "section-name",
                ),
                sectionFragment = SectionFragment(
                    sectionA = SectionFragment.SectionA(
                        id = "section-id",
                        imageUrl = "https://...",
                    ),
                ),
            ),
        ),
    )

    val data = apolloStore.readOperation(Issue2818Query())
    check(data.home.sectionA?.name == "section-name")
    check(data.home.sectionFragment.sectionA?.id == "section-id")
    check(data.home.sectionFragment.sectionA?.imageUrl == "https://...")
  }

  @Test
  // See https://github.com/apollographql/apollo-kotlin/issues/4772
  fun issue4772() = runTest {
    val mockserver = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockserver.url())
        .normalizedCache(MemoryCacheFactory())
        .build()

    mockserver.enqueue("""
      {
        "data": {
          "country": {
            "name": "Foo"
          }
        }
      }
    """.trimIndent())
    apolloClient.query(GetCountryQuery("foo")).execute().run {
      check(data?.country?.name == "Foo")
    }
    apolloClient.close()
    mockserver.stop()
  }
}
