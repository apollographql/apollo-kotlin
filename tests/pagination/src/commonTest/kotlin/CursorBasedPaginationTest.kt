package pagination

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.runTest
import pagination.test.UsersCursorBasedQuery_TestBuilder.Data
import kotlin.test.Test
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

  private fun cursorBased(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = CursorPaginationMetadataGenerator(),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = CursorPaginationRecordMerger()
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersCursorBasedQuery(first = Optional.Present(2))
    val data1 = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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

    // Page after
    val query2 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx43"))
    val data2 = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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
    var expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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

    // Page after
    val query3 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx45"))
    val data3 = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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
    expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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

    // Page before
    val query4 = UsersCursorBasedQuery(last = Optional.Present(2), before = Optional.Present("xx42"))
    val data4 = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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
    expectedData = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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

    // Non-contiguous page (should reset)
    val query5 = UsersCursorBasedQuery(first = Optional.Present(2), after = Optional.Present("xx50"))
    val data5 = UsersCursorBasedQuery.Data {
      usersCursorBased = usersCursorBased {
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
  }

  @Suppress("UNCHECKED_CAST")
  private class CursorPaginationMetadataGenerator : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.type.leafType().name == "UserConnection") {
        obj as Map<String, Any?>
        val edges = obj["edges"] as List<Map<String, Any?>>
        val startCursor = edges.first()["cursor"] as String
        val endCursor = edges.last()["cursor"] as String
        return mapOf(
            "startCursor" to startCursor,
            "endCursor" to endCursor,
            "arguments" to context.allArgumentValues()
        )
      }
      return emptyMap()
    }
  }

  @Suppress("UNCHECKED_CAST")
  private class CursorPaginationRecordMerger : RecordMerger {
    override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
      val changedKeys = mutableSetOf<String>()
      val mergedFields = existing.fields.toMutableMap()
      val mergedMetadata = existing.metadata.toMutableMap()
      val date = existing.date?.toMutableMap() ?: mutableMapOf()

      for ((fieldKey, incomingFieldValue) in incoming.fields) {
        val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
        val existingFieldValue = existing.fields[fieldKey]
        if (!hasExistingFieldValue || existingFieldValue != incomingFieldValue) {
          val existingStartCursor = existing.metadata[fieldKey]!!["startCursor"] as? String
          val existingEndCursor = existing.metadata[fieldKey]!!["endCursor"] as? String
          val incomingBeforeArgument = (incoming.metadata[fieldKey]!!["arguments"] as? Map<*, *>)?.get("before") as? String
          val incomingAfterArgument = (incoming.metadata[fieldKey]!!["arguments"] as? Map<*, *>)?.get("after") as? String

          if (existingStartCursor == null || existingEndCursor == null || incomingBeforeArgument == null && incomingAfterArgument == null) {
            mergedFields[fieldKey] = incomingFieldValue
            mergedMetadata[fieldKey] = incoming.metadata[fieldKey] as Map<String, Any?>
          } else {
            val existingList = (existing[fieldKey] as Map<*, *>)["edges"] as List<*>
            val incomingList = (incomingFieldValue as Map<*, *>)["edges"] as List<*>

            val mergedList: List<*>
            val newStartCursor: String
            val newEndCursor: String
            if (incomingAfterArgument == existingEndCursor) {
              mergedList = existingList + incomingList
              newStartCursor = existingStartCursor
              newEndCursor = incoming.metadata[fieldKey]!!["endCursor"] as String
            } else if (incomingBeforeArgument == existingStartCursor) {
              mergedList = incomingList + existingList
              newStartCursor = incoming.metadata[fieldKey]!!["startCursor"] as String
              newEndCursor = existingEndCursor
            } else {
              // We received a list which is neither the previous nor the next page.
              // Handle this case by resetting the cache with this page
              mergedList = incomingList
              newStartCursor = incoming.metadata[fieldKey]!!["startCursor"] as String
              newEndCursor = incoming.metadata[fieldKey]!!["endCursor"] as String
            }

            val mergedFieldValue = incomingFieldValue.toMutableMap()
            mergedFieldValue["edges"] = mergedList
            mergedFields[fieldKey] = mergedFieldValue
            mergedMetadata[fieldKey] = mapOf("startCursor" to newStartCursor, "endCursor" to newEndCursor)
          }
          changedKeys.add("${existing.key}.$fieldKey")
        }
        // Even if the value did not change update date
        if (newDate != null) {
          date[fieldKey] = newDate
        }
      }

      return Record(
          key = existing.key,
          fields = mergedFields,
          mutationId = incoming.mutationId,
          date = date,
          metadata = mergedMetadata,
      ) to changedKeys
    }
  }
}
