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
import kotlin.test.Test
import kotlin.test.assertEquals

class CursorPaginationTest {
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
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = cacheFactory,
            cacheKeyGenerator = TypePolicyCacheKeyGenerator,
            metadataGenerator = CursorPaginationMetadataGenerator(),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = CursorPaginationRecordMerger()
        )
        .serverUrl("unused")
        .build()
    client.apolloStore.clearAll()

    // First page
    val query1 = UsersCursorBasedQuery(Optional.Present(2))
    val data1 = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx42", UsersCursorBasedQuery.Node("42", "John", "john@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx43", UsersCursorBasedQuery.Node("43", "Jane", "jane@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query1, data1)
    var dataFromStore = client.apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = UsersCursorBasedQuery(Optional.Present(2), Optional.Present("xx43"))
    val data2 = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx44", UsersCursorBasedQuery.Node("44", "Peter", "peter@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx45", UsersCursorBasedQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query2, data2)
    dataFromStore = client.apolloStore.readOperation(query1)
    var expectedData = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx42", UsersCursorBasedQuery.Node("42", "John", "john@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx43", UsersCursorBasedQuery.Node("43", "Jane", "jane@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx44", UsersCursorBasedQuery.Node("44", "Peter", "peter@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx45", UsersCursorBasedQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page after
    val query3 = UsersCursorBasedQuery(Optional.Present(2), Optional.Present("xx45"))
    val data3 = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx46", UsersCursorBasedQuery.Node("46", "Bob", "bob@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx47", UsersCursorBasedQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query3, data3)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx42", UsersCursorBasedQuery.Node("42", "John", "john@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx43", UsersCursorBasedQuery.Node("43", "Jane", "jane@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx44", UsersCursorBasedQuery.Node("44", "Peter", "peter@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx45", UsersCursorBasedQuery.Node("45", "Alice", "alice@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx46", UsersCursorBasedQuery.Node("46", "Bob", "bob@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx47", UsersCursorBasedQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = UsersCursorBasedQuery(Optional.Absent, Optional.Absent, Optional.Present(2), Optional.Present("xx42"))
    val data4 = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx40", UsersCursorBasedQuery.Node("40", "Paul", "paul@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx41", UsersCursorBasedQuery.Node("41", "Mary", "mary@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query4, data4)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UsersCursorBasedQuery.Data(UsersCursorBasedQuery.UsersCursorBased(listOf(
        UsersCursorBasedQuery.Edge("xx40", UsersCursorBasedQuery.Node("40", "Paul", "paul@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx41", UsersCursorBasedQuery.Node("41", "Mary", "mary@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx42", UsersCursorBasedQuery.Node("42", "John", "john@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx43", UsersCursorBasedQuery.Node("43", "Jane", "jane@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx44", UsersCursorBasedQuery.Node("44", "Peter", "peter@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx45", UsersCursorBasedQuery.Node("45", "Alice", "alice@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx46", UsersCursorBasedQuery.Node("46", "Bob", "bob@a.com", "User")),
        UsersCursorBasedQuery.Edge("xx47", UsersCursorBasedQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)
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
            "endCursor" to endCursor
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
          val incomingBeforeArgument = incoming.arguments[fieldKey]!!["before"] as? String
          val incomingAfterArgument = incoming.arguments[fieldKey]!!["after"] as? String

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
          arguments = existing.arguments + incoming.arguments,
          metadata = mergedMetadata,
      ) to changedKeys
    }
  }
}
