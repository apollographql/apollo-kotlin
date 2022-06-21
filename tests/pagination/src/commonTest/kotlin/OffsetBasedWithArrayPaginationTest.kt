package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.runTest
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
    offsetBasedWithArray(SqlNormalizedCacheFactory(name = null, withDates = true))
  }

  @Test
  fun offsetBasedWithArrayJsonSqlCache() {
    offsetBasedWithArray(SqlNormalizedCacheFactory(name = null, withDates = false))
  }

  private fun offsetBasedWithArray(cacheFactory: NormalizedCacheFactory) = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = cacheFactory,
            cacheKeyGenerator = TypePolicyCacheKeyGenerator,
            metadataGenerator = OffsetPaginationMetadataGenerator("usersOffsetBasedWithArray"),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = OffsetPaginationRecordMerger()
        )
        .serverUrl("unused")
        .build()

    // First page
    val query1 = UsersOffsetBasedWithArrayQuery(Optional.Present(42), Optional.Present(2))
    val data1 = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("42", "John", "john@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("43", "Jane", "jane@a.com", "User"),
    ))
    client.apolloStore.writeOperation(query1, data1)
    var dataFromStore = client.apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = UsersOffsetBasedWithArrayQuery(Optional.Present(44), Optional.Present(2))
    val data2 = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("44", "Peter", "peter@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("45", "Alice", "alice@a.com", "User"),
    ))
    client.apolloStore.writeOperation(query2, data2)
    dataFromStore = client.apolloStore.readOperation(query1)
    var expectedData = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("42", "John", "john@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("43", "Jane", "jane@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("44", "Peter", "peter@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("45", "Alice", "alice@a.com", "User"),
    ))
    assertEquals(expectedData, dataFromStore)

    // Page in the middle
    val query3 = UsersOffsetBasedWithArrayQuery(Optional.Present(44), Optional.Present(3))
    val data3 = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("44", "Peter", "peter@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("45", "Alice", "alice@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("46", "Bob", "bob@a.com", "User"),
    ))
    client.apolloStore.writeOperation(query3, data3)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("42", "John", "john@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("43", "Jane", "jane@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("44", "Peter", "peter@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("45", "Alice", "alice@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("46", "Bob", "bob@a.com", "User"),
    ))
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = UsersOffsetBasedWithArrayQuery(Optional.Present(40), Optional.Present(2))
    val data4 = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("40", "Paul", "paul@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("41", "Mary", "mary@a.com", "User"),
    ))
    client.apolloStore.writeOperation(query4, data4)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithArrayQuery.Data(listOf(
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("40", "Paul", "paul@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("41", "Mary", "mary@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("42", "John", "john@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("43", "Jane", "jane@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("44", "Peter", "peter@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("45", "Alice", "alice@a.com", "User"),
        UsersOffsetBasedWithArrayQuery.UsersOffsetBasedWithArray("46", "Bob", "bob@a.com", "User"),
    ))
    assertEquals(expectedData, dataFromStore)
  }

  @Suppress("UNCHECKED_CAST")
  private class OffsetPaginationMetadataGenerator(private val fieldName: String) : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.name == fieldName) {
        return mapOf(
            "offset" to context.field.resolveArgument("offset", context.variables) as Int,
        )
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
          val incomingOffset = incoming.arguments[fieldKey]!!["offset"] as? Int
          if (existingOffset == null || incomingOffset == null) {
            mergedFields[fieldKey] = incomingFieldValue
            mergedMetadata[fieldKey] = incoming.metadata[fieldKey] as Map<String, Any?>
          } else {
            val existingList = existing[fieldKey] as List<*>
            val incomingList = incomingFieldValue as List<*>
            val mergedList = mergeLists(existingList, incomingList, existingOffset, incomingOffset)
            mergedFields[fieldKey] = mergedList
            mergedMetadata[fieldKey] = mapOf("offset" to min(existingOffset, incomingOffset))
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
          arguments = existing.arguments + incoming.arguments,
          metadata = mergedMetadata,
      ) to changedKeys
    }

    private fun <T> mergeLists(existing: List<T>, incoming: List<T>, existingOffset: Int, incomingOffset: Int): List<T> {
      if (incomingOffset > existingOffset + existing.size) {
        // Incoming list's first item is further than immediately after the existing list's last item: can't merge. Handle it as a reset.
        return incoming
      }

      if (incomingOffset + incoming.size < existingOffset) {
        // Incoming list's last item is further than immediately before the existing list's first item: can't merge. Handle it as a reset.
        return incoming
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
      return merged
    }
  }
}
