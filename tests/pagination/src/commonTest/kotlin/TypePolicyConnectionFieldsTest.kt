package pagination

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.ConnectionMetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.ConnectionRecordMerger
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.internal.runTest
import pagination.pagination.Pagination
import pagination.test.WithTypePolicyDirectiveQuery_TestBuilder.Data
import kotlin.test.Test
import kotlin.test.assertEquals

class TypePolicyConnectionFieldsTest {
  @Test
  fun typePolicyConnectionFieldsMemoryCache() {
    typePolicyConnectionFields(MemoryCacheFactory())
  }

  @Test
  fun typePolicyConnectionFieldsBlobSqlCache() {
    typePolicyConnectionFields(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun typePolicyConnectionFieldsJsonSqlCache() {
    typePolicyConnectionFields(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  @Test
  fun typePolicyConnectionFieldsChainedCache() {
    typePolicyConnectionFields(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(name = "json", withDates = false)))
  }

  private fun typePolicyConnectionFields(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = ConnectionMetadataGenerator(Pagination.connectionTypes),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = ConnectionRecordMerger
    )
    apolloStore.clearAll()

    // First page
    val query1 = WithTypePolicyDirectiveQuery(first = Optional.Present(2))
    val data1 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx42"
              node = node {
                id = "42"
              }
            },
            edge {
              cursor = "xx43"
              node = node {
                id = "43"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query1, data1)
    var dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page after
    val query2 = WithTypePolicyDirectiveQuery(first = Optional.Present(2), after = Optional.Present("xx43"))
    val data2 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx44"
              node = node {
                id = "44"
              }
            },
            edge {
              cursor = "xx45"
              node = node {
                id = "45"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query2, data2)
    dataFromStore = apolloStore.readOperation(query1)
    var expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx42"
              node = node {
                id = "42"
              }
            },
            edge {
              cursor = "xx43"
              node = node {
                id = "43"
              }
            },
            edge {
              cursor = "xx44"
              node = node {
                id = "44"
              }
            },
            edge {
              cursor = "xx45"
              node = node {
                id = "45"
              }
            },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page after
    val query3 = WithTypePolicyDirectiveQuery(first = Optional.Present(2), after = Optional.Present("xx45"))
    val data3 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx46"
              node = node {
                id = "46"
              }
            },
            edge {
              cursor = "xx47"
              node = node {
                id = "47"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query3, data3)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx42"
              node = node {
                id = "42"
              }
            },
            edge {
              cursor = "xx43"
              node = node {
                id = "43"
              }
            },
            edge {
              cursor = "xx44"
              node = node {
                id = "44"
              }
            },
            edge {
              cursor = "xx45"
              node = node {
                id = "45"
              }
            },
            edge {
              cursor = "xx46"
              node = node {
                id = "46"
              }
            },
            edge {
              cursor = "xx47"
              node = node {
                id = "47"
              }
            },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page before
    val query4 = WithTypePolicyDirectiveQuery(last = Optional.Present(2), before = Optional.Present("xx42"))
    val data4 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx40"
              node = node {
                id = "40"
              }
            },
            edge {
              cursor = "xx41"
              node = node {
                id = "41"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query4, data4)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx40"
              node = node {
                id = "40"
              }
            },
            edge {
              cursor = "xx41"
              node = node {
                id = "41"
              }
            },
            edge {
              cursor = "xx42"
              node = node {
                id = "42"
              }
            },
            edge {
              cursor = "xx43"
              node = node {
                id = "43"
              }
            },
            edge {
              cursor = "xx44"
              node = node {
                id = "44"
              }
            },
            edge {
              cursor = "xx45"
              node = node {
                id = "45"
              }
            },
            edge {
              cursor = "xx46"
              node = node {
                id = "46"
              }
            },
            edge {
              cursor = "xx47"
              node = node {
                id = "47"
              }
            },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Non-contiguous page (should reset)
    val query5 = WithTypePolicyDirectiveQuery(first = Optional.Present(2), after = Optional.Present("xx50"))
    val data5 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = listOf(
            edge {
              cursor = "xx50"
              node = node {
                id = "50"
              }
            },
            edge {
              cursor = "xx51"
              node = node {
                id = "51"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query5, data5)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Empty page (should keep previous result)
    val query6 = WithTypePolicyDirectiveQuery(first = Optional.Present(2), after = Optional.Present("xx51"))
    val data6 = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = usersCursorBased2 {
        edges = emptyList()
      }
    }
    apolloStore.writeOperation(query6, data6)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)
  }
}

