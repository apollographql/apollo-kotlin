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
import pagination.type.buildUser
import pagination.type.buildUserConnection2
import pagination.type.buildUserEdge
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
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx42"
              node = buildUser {
                id = "42"
              }
            },
            buildUserEdge {
              cursor = "xx43"
              node = buildUser {
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
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx44"
              node = buildUser {
                id = "44"
              }
            },
            buildUserEdge {
              cursor = "xx45"
              node = buildUser {
                id = "45"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query2, data2)
    dataFromStore = apolloStore.readOperation(query1)
    var expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx42"
              node = buildUser {
                id = "42"
              }
            },
            buildUserEdge {
              cursor = "xx43"
              node = buildUser {
                id = "43"
              }
            },
            buildUserEdge {
              cursor = "xx44"
              node = buildUser {
                id = "44"
              }
            },
            buildUserEdge {
              cursor = "xx45"
              node = buildUser {
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
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx46"
              node = buildUser {
                id = "46"
              }
            },
            buildUserEdge {
              cursor = "xx47"
              node = buildUser {
                id = "47"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query3, data3)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx42"
              node = buildUser {
                id = "42"
              }
            },
            buildUserEdge {
              cursor = "xx43"
              node = buildUser {
                id = "43"
              }
            },
            buildUserEdge {
              cursor = "xx44"
              node = buildUser {
                id = "44"
              }
            },
            buildUserEdge {
              cursor = "xx45"
              node = buildUser {
                id = "45"
              }
            },
            buildUserEdge {
              cursor = "xx46"
              node = buildUser {
                id = "46"
              }
            },
            buildUserEdge {
              cursor = "xx47"
              node = buildUser {
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
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx40"
              node = buildUser {
                id = "40"
              }
            },
            buildUserEdge {
              cursor = "xx41"
              node = buildUser {
                id = "41"
              }
            },
        )
      }
    }
    apolloStore.writeOperation(query4, data4)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = WithTypePolicyDirectiveQuery.Data {
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx40"
              node = buildUser {
                id = "40"
              }
            },
            buildUserEdge {
              cursor = "xx41"
              node = buildUser {
                id = "41"
              }
            },
            buildUserEdge {
              cursor = "xx42"
              node = buildUser {
                id = "42"
              }
            },
            buildUserEdge {
              cursor = "xx43"
              node = buildUser {
                id = "43"
              }
            },
            buildUserEdge {
              cursor = "xx44"
              node = buildUser {
                id = "44"
              }
            },
            buildUserEdge {
              cursor = "xx45"
              node = buildUser {
                id = "45"
              }
            },
            buildUserEdge {
              cursor = "xx46"
              node = buildUser {
                id = "46"
              }
            },
            buildUserEdge {
              cursor = "xx47"
              node = buildUser {
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
      usersCursorBased2 = buildUserConnection2 {
        edges = listOf(
            buildUserEdge {
              cursor = "xx50"
              node = buildUser {
                id = "50"
              }
            },
            buildUserEdge {
              cursor = "xx51"
              node = buildUser {
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
      usersCursorBased2 = buildUserConnection2 {
        edges = emptyList()
      }
    }
    apolloStore.writeOperation(query6, data6)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)
  }
}

