package pagination

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.FieldRecordMerger
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.runTest
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
        metadataGenerator = CursorPaginationMetadataGenerator(Pagination.connectionTypes),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = FieldRecordMerger(CursorPaginationFieldMerger())
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

  @Suppress("UNCHECKED_CAST")
  private class CursorPaginationMetadataGenerator(private val connectionTypes: Set<String>) : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.type.leafType().name in connectionTypes) {
        obj as Map<String, Any?>
        val edges = obj["edges"] as List<Map<String, Any?>>
        val startCursor = edges.firstOrNull()?.get("cursor") as String?
        val endCursor = edges.lastOrNull()?.get("cursor") as String?
        return mapOf(
            "startCursor" to startCursor,
            "endCursor" to endCursor,
            "before" to context.argumentValue("before"),
            "after" to context.argumentValue("after"),
        )
      }
      return emptyMap()
    }
  }

  private class CursorPaginationFieldMerger : FieldRecordMerger.FieldMerger {
    @Suppress("UNCHECKED_CAST")
    override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
      val existingStartCursor = existing.metadata["startCursor"] as? String
      val existingEndCursor = existing.metadata["endCursor"] as? String
      val incomingStartCursor = incoming.metadata["startCursor"] as? String
      val incomingEndCursor = incoming.metadata["endCursor"] as? String
      val incomingBeforeArgument = incoming.metadata["before"] as? String
      val incomingAfterArgument = incoming.metadata["after"] as? String

      return if (incomingBeforeArgument == null && incomingAfterArgument == null) {
        // Not a pagination query
        incoming
      } else if (existingStartCursor == null || existingEndCursor == null) {
        // Existing is empty
        incoming
      } else if (incomingStartCursor == null || incomingEndCursor == null) {
        // Incoming is empty
        existing
      } else {
        val existingValue = existing.value as Map<String, Any?>
        val existingList = existingValue["edges"] as List<*>
        val incomingList = (incoming.value as Map<String, Any?>)["edges"] as List<*>

        val mergedList: List<*>
        val newStartCursor: String
        val newEndCursor: String
        if (incomingAfterArgument == existingEndCursor) {
          mergedList = existingList + incomingList
          newStartCursor = existingStartCursor
          newEndCursor = incomingEndCursor
        } else if (incomingBeforeArgument == existingStartCursor) {
          mergedList = incomingList + existingList
          newStartCursor = incomingStartCursor
          newEndCursor = existingEndCursor
        } else {
          // We received a list which is neither the previous nor the next page.
          // Handle this case by resetting the cache with this page
          mergedList = incomingList
          newStartCursor = incomingStartCursor
          newEndCursor = incomingEndCursor
        }

        val mergedFieldValue = existingValue.toMutableMap()
        mergedFieldValue["edges"] = mergedList
        FieldRecordMerger.FieldInfo(
            value = mergedFieldValue,
            metadata = mapOf("startCursor" to newStartCursor, "endCursor" to newEndCursor)
        )
      }
    }
  }
}

