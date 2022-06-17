package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
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
            metadataGenerator = CursorPaginationMetadataGenerator(),
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

class CursorPaginationMetadataGenerator : MetadataGenerator {
  override fun metadataForObject(obj: Map<String, Any?>, context: MetadataGeneratorContext): Map<String, Any?> {
    if (context.field.type.leafType().name == "UserConnection") {
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

class AppendListRecordMerger : RecordMerger {
  override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
    val existingStartCursor = existing.metadata["startCursor"] as? String
    val existingEndCursor = existing.metadata["endCursor"] as? String
    if (existingStartCursor == null || existingEndCursor == null) {
      return DefaultRecordMerger.merge(existing, incoming, newDate)
    }

    val incomingBeforeArgument = incoming.arguments["before"] as? String
    val incomingAfterArgument = incoming.arguments["after"] as? String
    if (incomingBeforeArgument == null && incomingAfterArgument == null) {
      return DefaultRecordMerger.merge(existing, incoming, newDate)
    }

    val existingEdges = existing["edges"] as List<Record>
    val incomingEdges = incoming["edges"] as List<Record>
    val mergedEdges: List<Record>
    val newStartCursor: String
    val newEndCursor: String
    if (incomingAfterArgument == existingEndCursor) {
      mergedEdges = existingEdges + incomingEdges
      newStartCursor = existingStartCursor
      newEndCursor = incoming.metadata["endCursor"] as String
    } else if (incomingBeforeArgument == existingStartCursor) {
      mergedEdges = incomingEdges + existingEdges
      newStartCursor = incoming.metadata["startCursor"] as String
      newEndCursor = existingEndCursor
    } else {
      // We received a list which is neither the previous nor the next page.
      // Handle this case by resetting the cache with this page
      mergedEdges = incomingEdges
      newStartCursor = incoming.metadata["startCursor"] as String
      newEndCursor = incoming.metadata["endCursor"] as String
    }
    val mergedFields = mapOf(
        "edges" to mergedEdges,
        // TODO: Use the incoming pageInfo - this may be unexpected
        "pageInfo" to incoming["pageInfo"],
    )
    val changedKeys = setOf("edges", "pageInfo")
    val date = existing.date?.toMutableMap() ?: mutableMapOf()
    if (newDate != null) {
      date["edges"] = newDate
      date["pageInfo"] = newDate
    }
    val metadata = mapOf("startCursor" to newStartCursor, "endCursor" to newEndCursor)

    return Record(
        key = existing.key,
        fields = mergedFields,
        mutationId = incoming.mutationId,
        date = date,
        arguments = emptyMap(),
        metadata = metadata,
    ) to changedKeys
  }
}
