package pagination

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.api.ConnectionEmbeddedFieldsProvider
import com.apollographql.cache.normalized.api.ConnectionFieldKeyGenerator
import com.apollographql.cache.normalized.api.ConnectionMetadataGenerator
import com.apollographql.cache.normalized.api.ConnectionRecordMerger
import com.apollographql.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.cache.normalized.api.MemoryCacheFactory
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import pagination.connectionProgrammatic.UsersQuery
import pagination.connectionProgrammatic.type.Query
import pagination.connectionProgrammatic.type.UserConnection
import pagination.connectionProgrammatic.type.buildPageInfo
import pagination.connectionProgrammatic.type.buildUser
import pagination.connectionProgrammatic.type.buildUserConnection
import pagination.connectionProgrammatic.type.buildUserEdge
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionProgrammaticPaginationTest {
  @Test
  fun memoryCache() {
    test(MemoryCacheFactory())
  }

  @Test
  fun blobSqlCache() {
    test(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun jsonSqlCache() {
    test(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  @Test
  fun chainedCache() {
    test(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(name = "json", withDates = false)))
  }

  private fun test(cacheFactory: NormalizedCacheFactory) = runTest {
    val connectionTypes = setOf(UserConnection.type.name)
    val connectionFields = mapOf(Query.type.name to listOf("users"))
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = ConnectionMetadataGenerator(connectionTypes),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = ConnectionRecordMerger,
        fieldKeyGenerator = ConnectionFieldKeyGenerator(connectionFields),
        embeddedFieldsProvider = ConnectionEmbeddedFieldsProvider(
            connectionTypes = connectionTypes,
            connectionFields = connectionFields
        ),
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersQuery(first = Optional.Present(2))
    val data1 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx42"
          endCursor = "xx43"
        }
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
    val query2 = UsersQuery(first = Optional.Present(2), after = Optional.Present("xx43"))
    val data2 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx44"
          endCursor = "xx45"
        }
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
    var expectedData = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx42"
          endCursor = "xx45"
        }
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
    val query3 = UsersQuery(first = Optional.Present(2), after = Optional.Present("xx45"))
    val data3 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx46"
          endCursor = "xx47"
        }
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
    expectedData = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx42"
          endCursor = "xx47"
        }
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
    val query4 = UsersQuery(last = Optional.Present(2), before = Optional.Present("xx42"))
    val data4 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx40"
          endCursor = "xx41"
        }
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
    expectedData = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx40"
          endCursor = "xx47"
        }
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
    val query5 = UsersQuery(first = Optional.Present(2), after = Optional.Present("xx50"))
    val data5 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = "xx50"
          endCursor = "xx51"
        }
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
    val query6 = UsersQuery(first = Optional.Present(2), after = Optional.Present("xx51"))
    val data6 = UsersQuery.Data {
      users = buildUserConnection {
        pageInfo = buildPageInfo {
          startCursor = null
          endCursor = null
        }
        edges = emptyList()
      }
    }
    apolloStore.writeOperation(query6, data6)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)
  }
}

