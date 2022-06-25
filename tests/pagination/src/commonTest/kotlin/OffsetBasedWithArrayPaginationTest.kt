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
import pagination.test.UsersOffsetBasedWithArrayQuery_TestBuilder.Data
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetBasedWithArrayPaginationTest {
  @Test
  fun offsetBasedWithArrayMemoryCache() {
    offsetBasedWithArray(MemoryCacheFactory())
  }

  @Test
  fun offsetBasedWithArrayBlobSqlCache() {
    offsetBasedWithArray(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun offsetBasedWithArrayJsonSqlCache() {
    offsetBasedWithArray(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  private fun offsetBasedWithArray(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = OffsetPaginationMetadataGenerator("usersOffsetBasedWithArray"),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = OffsetPaginationRecordMerger()
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersOffsetBasedWithArrayQuery(offset = Optional.Present(42), limit = Optional.Present(2))
    val data1 = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "42" },
          usersOffsetBasedWithArray { id = "43" },
      )
    }
    apolloStore.writeOperation(query1, data1)
    var dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = UsersOffsetBasedWithArrayQuery(offset = Optional.Present(44), limit = Optional.Present(2))
    val data2 = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "44" },
          usersOffsetBasedWithArray { id = "45" },
      )
    }
    apolloStore.writeOperation(query2, data2)
    dataFromStore = apolloStore.readOperation(query1)
    var expectedData = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "42" },
          usersOffsetBasedWithArray { id = "43" },
          usersOffsetBasedWithArray { id = "44" },
          usersOffsetBasedWithArray { id = "45" },
      )
    }
    assertEquals(expectedData, dataFromStore)

    // Page in the middle
    val query3 = UsersOffsetBasedWithArrayQuery(offset = Optional.Present(44), limit = Optional.Present(3))
    val data3 = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "44" },
          usersOffsetBasedWithArray { id = "45" },
          usersOffsetBasedWithArray { id = "46" },
      )
    }
    apolloStore.writeOperation(query3, data3)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "42" },
          usersOffsetBasedWithArray { id = "43" },
          usersOffsetBasedWithArray { id = "44" },
          usersOffsetBasedWithArray { id = "45" },
          usersOffsetBasedWithArray { id = "46" },
      )
    }
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = UsersOffsetBasedWithArrayQuery(offset = Optional.Present(40), limit = Optional.Present(2))
    val data4 = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "40" },
          usersOffsetBasedWithArray { id = "41" },
      )
    }
    apolloStore.writeOperation(query4, data4)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "40" },
          usersOffsetBasedWithArray { id = "41" },
          usersOffsetBasedWithArray { id = "42" },
          usersOffsetBasedWithArray { id = "43" },
          usersOffsetBasedWithArray { id = "44" },
          usersOffsetBasedWithArray { id = "45" },
          usersOffsetBasedWithArray { id = "46" },
      )
    }
    assertEquals(expectedData, dataFromStore)

    // Non-contiguous page (should reset)
    val query5 = UsersOffsetBasedWithArrayQuery(offset = Optional.Present(50), limit = Optional.Present(2))
    val data5 = UsersOffsetBasedWithArrayQuery.Data {
      usersOffsetBasedWithArray = listOf(
          usersOffsetBasedWithArray { id = "50" },
          usersOffsetBasedWithArray { id = "51" },
      )
    }
    apolloStore.writeOperation(query5, data5)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
  }

  @Suppress("UNCHECKED_CAST")
  private class OffsetPaginationMetadataGenerator(private val fieldName: String) : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.name == fieldName) {
        return mapOf("offset" to context.argumentValue("offset"))
      }
      return emptyMap()
    }
  }

  @Suppress("UNCHECKED_CAST")
  private class OffsetPaginationRecordMerger : RecordMerger {
    override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
      val changedKeys = mutableSetOf<String>()
      val mergedFields = existing.fields.toMutableMap()
      val mergedMetadata = existing.metadata.toMutableMap()
      val date = existing.date?.toMutableMap() ?: mutableMapOf()

      for ((fieldKey, incomingFieldValue) in incoming.fields) {
        val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
        val existingFieldValue = existing.fields[fieldKey]
        if (!hasExistingFieldValue || existingFieldValue != incomingFieldValue) {
          val existingOffset = existing.metadata[fieldKey]!!["offset"] as? Int
          val incomingOffset = incoming.metadata[fieldKey]!!["offset"] as? Int
          if (existingOffset == null || incomingOffset == null) {
            mergedFields[fieldKey] = incomingFieldValue
            mergedMetadata[fieldKey] = incoming.metadata[fieldKey] as Map<String, Any?>
          } else {
            val existingList = existing[fieldKey] as List<*>
            val incomingList = incomingFieldValue as List<*>
            val (mergedList, mergedOffset) = mergeLists(existingList, incomingList, existingOffset, incomingOffset)
            mergedFields[fieldKey] = mergedList
            mergedMetadata[fieldKey] = mapOf("offset" to mergedOffset)
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

    private fun <T> mergeLists(existing: List<T>, incoming: List<T>, existingOffset: Int, incomingOffset: Int): Pair<List<T>, Int> {
      if (incomingOffset > existingOffset + existing.size) {
        // Incoming list's first item is further than immediately after the existing list's last item: can't merge. Handle it as a reset.
        return incoming to incomingOffset
      }

      if (incomingOffset + incoming.size < existingOffset) {
        // Incoming list's last item is further than immediately before the existing list's first item: can't merge. Handle it as a reset.
        return incoming to incomingOffset
      }

      val merged = mutableListOf<T>()
      val startOffset = min(existingOffset, incomingOffset)
      val endOffset = max(existingOffset + existing.size, incomingOffset + incoming.size)
      val incomingRange = incomingOffset until incomingOffset + incoming.size
      for (i in startOffset until endOffset) {
        if (i in incomingRange) {
          merged.add(incoming[i - incomingOffset])
        } else {
          merged.add(existing[i - existingOffset])
        }
      }
      return merged to startOffset
    }
  }
}
