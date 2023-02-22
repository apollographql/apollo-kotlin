package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.ConnectionMetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.ConnectionRecordMerger
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.QueueTestNetworkTransport
import com.apollographql.apollo3.testing.enqueueTestResponse
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.toList
import pagination.type.buildUser
import pagination.type.buildUserConnection
import pagination.type.buildUserEdge
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CursorBasedPaginationTest {
  @Test
  fun cursorBasedMemoryCache() {
    cursorBased(MemoryCacheFactory())
  }

  @Test
  fun cursorBasedBlobSqlCache() {
    cursorBased(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun cursorBasedJsonSqlCache() {
    cursorBased(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  @Test
  fun cursorBasedChainedCache() {
    cursorBased(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(name = "json", withDates = false)))
  }

  private fun cursorBased(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = ConnectionMetadataGenerator(setOf("UserConnection")),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = ConnectionRecordMerger
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersCursorBasedQuery(first = Optional.Present(2))
    val data1 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    val query2 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx43"))
    val data2 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    var expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    val query3 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx45"))
    val data3 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    val query4 = UsersCursorBasedQuery(last = Optional.Present(2), before = Optional.Present("xx42"))
    val data4 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    val query5 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx50"))
    val data5 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    val query6 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx51"))
    val data6 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
        edges = emptyList()
      }
    }
    apolloStore.writeOperation(query6, data6)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)
  }

  @Test
  fun cacheAndNetworkPolicy() = runTest {
    val apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .normalizedCache(
            normalizedCacheFactory = MemoryCacheFactory(),
            cacheKeyGenerator = TypePolicyCacheKeyGenerator,
            metadataGenerator = ConnectionMetadataGenerator(setOf("UserConnection")),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = ConnectionRecordMerger
        )
        .build()

    // First page
    val query1 = UsersCursorBasedQuery(first = Optional.Present(2))
    val data1 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    apolloClient.enqueueTestResponse(query1, data1)
    var resultData = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList().map { it.data }
    // Cache: null
    // Network: data1
    assertContentEquals(listOf(null, data1), resultData)


    // Page after
    val query2 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx43"))
    val data2 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    apolloClient.enqueueTestResponse(query2, data2)
    resultData = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList().map { it.data }
    // Cache: data1
    // Network: data2
    assertContentEquals(listOf(data1, data2), resultData)


    // Page after
    val query3 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx45"))
    val data3 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    apolloClient.enqueueTestResponse(query3, data3)
    resultData = apolloClient.query(query3).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList().map { it.data }
    // Cache: data1 merged with data2
    val data1MergedWith2 = UsersCursorBasedQuery.Data {
      usersCursorBased = buildUserConnection {
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
    // Network: data3
    assertContentEquals(listOf(data1MergedWith2, data3), resultData)
  }

}

