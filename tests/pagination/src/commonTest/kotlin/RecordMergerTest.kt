package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordMergerTest {
  @Test
  fun customRecordMerger() = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = MemoryCacheFactory(),
            cacheKeyGenerator = TypePolicyCacheKeyGenerator,
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = AppendListRecordMerger()
        )
        .serverUrl("unused")
        .build()
    val query = UserListQuery()
    val data1 = UserListQuery.Data(listOf(
        UserListQuery.User("0", "John", "john@a.com", "User"),
        UserListQuery.User("1", "Jane", "jane@a.com", "User"),
    ))
    val data2 = UserListQuery.Data(listOf(
        UserListQuery.User("2", "Peter", "peter@a.com", "User"),
        UserListQuery.User("3", "Alice", "alice@a.com", "User"),
    ))
    client.apolloStore.writeOperation(query, data1)
    client.apolloStore.writeOperation(query, data2)

    val dataFromStore = client.apolloStore.readOperation(query)
    val mergedData = UserListQuery.Data(listOf(
        UserListQuery.User("0", "John", "john@a.com", "User"),
        UserListQuery.User("1", "Jane", "jane@a.com", "User"),
        UserListQuery.User("2", "Peter", "peter@a.com", "User"),
        UserListQuery.User("3", "Alice", "alice@a.com", "User"),
    ))
    assertEquals(mergedData, dataFromStore)
  }
}

class AppendListRecordMerger : RecordMerger {
  override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
    val changedKeys = mutableSetOf<String>()
    val mergedFields = existing.fields.toMutableMap()
    val date = existing.date?.toMutableMap() ?: mutableMapOf()

    for ((fieldKey, incomingFieldValue) in incoming.fields) {
      val hasExistingFieldValue = existing.fields.containsKey(fieldKey)
      val existingFieldValue = existing.fields[fieldKey]

      if (!hasExistingFieldValue) {
        mergedFields[fieldKey] = incomingFieldValue
        changedKeys.add("${existing.key}.$fieldKey")
      } else if (incomingFieldValue is List<*> && existingFieldValue is List<*>) {
        mergedFields[fieldKey] = existingFieldValue + incomingFieldValue
        changedKeys.add("${existing.key}.$fieldKey")
      } else if (existingFieldValue != incomingFieldValue) {
        mergedFields[fieldKey] = incomingFieldValue
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
        date = date
    ) to changedKeys
  }
}
