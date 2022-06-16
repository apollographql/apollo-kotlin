package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
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
            cacheKeyGenerator = IgnoreArgumentsOnConnectionKeyGenerator(),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = AppendListRecordMerger()
        )
        .serverUrl("unused")
        .build()
    val query1 = UserListQuery(Optional.Present(null), Optional.Present(2))
    val data1 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("www", UserListQuery.Node("0", "John", "john@a.com", "User")),
        UserListQuery.Edge("xxx", UserListQuery.Node("1", "Jane", "jane@a.com", "User")),
    )))
    val query2 = UserListQuery(Optional.Present("xxx"), Optional.Present(2))
    val data2 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("yyy", UserListQuery.Node("2", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("zzz", UserListQuery.Node("3", "Alice", "alice@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query1, data1)
    client.apolloStore.writeOperation(query2, data2)

    val dataFromStore = client.apolloStore.readOperation(query1)
    val mergedData = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("www", UserListQuery.Node("0", "John", "john@a.com", "User")),
        UserListQuery.Edge("xxx", UserListQuery.Node("1", "Jane", "jane@a.com", "User")),
        UserListQuery.Edge("yyy", UserListQuery.Node("2", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("zzz", UserListQuery.Node("3", "Alice", "alice@a.com", "User")),
    )))
    assertEquals(mergedData, dataFromStore)
  }
}

class IgnoreArgumentsOnConnectionKeyGenerator : CacheKeyGenerator {
  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
    if (context.field.type.leafType().name == "UserConnection") {
      return CacheKey("users")
    }
    return TypePolicyCacheKeyGenerator.cacheKeyForObject(obj, context)
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
        date = date,
        arguments = existing.arguments,
        metadata = incoming.metadata + existing.metadata,
    ) to changedKeys
  }
}
