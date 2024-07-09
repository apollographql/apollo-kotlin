package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.toApolloResponse
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.api.CacheResolver
import com.apollographql.apollo.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import com.example.one.Issue2818Query
import com.example.one.Issue3672Query
import com.example.one.fragment.SectionFragment
import com.example.three.GetBooksByIdsPaginatedNoCursorsQuery
import com.example.three.GetBooksByIdsPaginatedNoCursorsWithFragmentQuery
import com.example.three.GetBooksByIdsPaginatedQuery
import com.example.three.GetBooksByIdsQuery
import com.example.three.type.Book
import com.example.three.type.BookConnection
import com.example.three.type.BookEdge
import com.example.two.GetCountryQuery
import com.example.two.NestedFragmentQuery
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals

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

    val data1 = Buffer().writeUtf8(nestedResponse).jsonReader().toApolloResponse(operation = query, customScalarAdapters = CustomScalarAdapters.Empty).dataOrThrow()
    store.writeOperationSync(query, data1)

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

    val data1 = Buffer().writeUtf8(nestedResponse_list).jsonReader().toApolloResponse(operation = query, customScalarAdapters = CustomScalarAdapters.Empty).dataOrThrow()
    store.writeOperationSync(query, data1)

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

    apolloStore.writeOperationSync(
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

    mockserver.enqueueString("""
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
    mockserver.close()
  }

  @Test
  fun resolveList() = runTest {
    val mockserver = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockserver.url())
        .store(
            ApolloStore(
                normalizedCacheFactory = MemoryCacheFactory(),
                cacheKeyGenerator = TypePolicyCacheKeyGenerator,
                cacheResolver = object : CacheKeyResolver() {
                  override fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey? {
                    // Same behavior as FieldPolicyCacheResolver
                    val keyArgsValues = field.argumentValues(variables) { it.definition.isKey }.values.map { it.toString() }
                    if (keyArgsValues.isNotEmpty()) {
                      return CacheKey(field.type.rawType().name, keyArgsValues)
                    }
                    return null
                  }

                  @Suppress("UNCHECKED_CAST")
                  override fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>? {
                    return if (field.type.rawType() == Book.type) {
                      val bookIds = field.argumentValues(variables)["bookIds"] as List<String>
                      bookIds.map { CacheKey(Book.type.name, it) }
                    } else {
                      null
                    }
                  }
                }
            )
        )
        .build()

    mockserver.enqueueString("""
      {
        "data": {
          "viewer": {
            "libraries": [
              {
                "__typename": "Library",
                "id": "library-1",
                "books": [
                  {
                    "__typename": "Book",
                    "id": "book-1",
                    "name": "First book",
                    "year": 1991
                  },
                  {
                    "__typename": "Book",
                    "id": "book-2",
                    "name": "Second book",
                    "year": 1992
                  }
                ]
              }
            ]
          }
        }
      }
    """.trimIndent())

    // Fetch from network
    apolloClient.query(GetBooksByIdsQuery(listOf("book-1", "book-2"))).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    // Fetch from the cache
    val fromCache = apolloClient.query(GetBooksByIdsQuery(listOf("book-1"))).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals("First book", fromCache.data?.viewer?.libraries?.first()?.books?.first()?.name)

    apolloClient.close()
    mockserver.close()
  }

  @Test
  fun resolvePaginatedList() = runTest {
    val mockserver = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockserver.url())
        .store(
            ApolloStore(
                normalizedCacheFactory = MemoryCacheFactory(),
                cacheKeyGenerator = TypePolicyCacheKeyGenerator,
                cacheResolver = object : CacheResolver {
                  @Suppress("UNCHECKED_CAST")
                  override fun resolveField(
                      field: CompiledField,
                      variables: Executable.Variables,
                      parent: Map<String, Any?>,
                      parentId: String,
                  ): Any? {
                    if (field.type.rawType() == BookConnection.type) {
                      val bookIds = field.argumentValues(variables)["bookIds"] as List<String>
                      return mapOf(
                          "edges" to bookIds.map {
                            mapOf(
                                "node" to CacheKey(Book.type.name, it),
                                "__typename" to BookEdge.type.name,
                            )
                          },
                      )
                    }

                    return FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
                  }
                }

            )
        )
        .build()

    mockserver.enqueueString("""
      {
        "data": {
          "viewer": {
            "libraries": [
              {
                "__typename": "Library",
                "id": "library-1",
                "booksPaginated": {
                  "pageInfo": {
                    "__typename": "PageInfo",
                    "hasNextPage": false,
                    "endCursor": "book-2"
                  },
                  "edges": [
                    {
                      "__typename": "BookEdge",
                      "cursor": "cursor-book-1",
                      "node": {
                        "__typename": "Book",
                        "id": "book-1",
                        "name": "First book",
                        "year": 1991
                      }
                    },
                    {
                      "__typename": "BookEdge",
                      "cursor": "cursor-book-2",
                      "node": {
                        "__typename": "Book",
                        "id": "book-2",
                        "name": "Second book",
                        "year": 1992
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
      }
    """.trimIndent())

    // Fetch from network
    apolloClient.query(GetBooksByIdsPaginatedQuery(listOf("book-1", "book-2"))).fetchPolicy(FetchPolicy.NetworkOnly).execute()
    // println(NormalizedCache.prettifyDump(apolloClient.apolloStore.dump()))

    // Fetch from the cache
    val fromCache1 = apolloClient.query(GetBooksByIdsPaginatedNoCursorsQuery(listOf("book-1"))).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals("First book", fromCache1.data?.viewer?.libraries?.first()?.booksPaginated?.edges?.first()?.node?.name)

    // Fetch from the cache (with fragment)
    val fromCache2 = apolloClient.query(GetBooksByIdsPaginatedNoCursorsWithFragmentQuery(listOf("book-1"))).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals("First book", fromCache2.data?.viewer?.libraries?.first()?.booksPaginated?.edges?.first()?.bookEdge?.node?.name)

    apolloClient.close()
    mockserver.close()
  }
}
