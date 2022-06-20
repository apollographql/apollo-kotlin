package pagination

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
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

class RecordMergerTest {
  @Test
  fun customRecordMergerMemoryCache() {
    customRecordMerger(MemoryCacheFactory())
  }

  @Test
  fun customRecordMergerBlobSqlCache() {
    customRecordMerger(SqlNormalizedCacheFactory(name = null, withDates = true))
  }

  @Test
  fun customRecordMergerJsonSqlCache() {
    customRecordMerger(SqlNormalizedCacheFactory(name = null, withDates = false))
  }

  private fun customRecordMerger(cacheFactory: NormalizedCacheFactory) = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = cacheFactory,
            cacheKeyGenerator = IgnoreArgumentsOnConnectionKeyGenerator(),
            metadataGenerator = CursorPaginationMetadataGenerator(),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = CursorPaginationRecordMerger()
        )
        .serverUrl("unused")
        .build()

    // First page
    val query1 = UserListQuery(Optional.Present(2))
    val data1 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx42", UserListQuery.Node("42", "John", "john@a.com", "User")),
        UserListQuery.Edge("xx43", UserListQuery.Node("43", "Jane", "jane@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query1, data1)
    var dataFromStore = client.apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = UserListQuery(Optional.Present(2), Optional.Present("xx43"))
    val data2 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx44", UserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("xx45", UserListQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query2, data2)
    dataFromStore = client.apolloStore.readOperation(query1)
    var expectedData = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx42", UserListQuery.Node("42", "John", "john@a.com", "User")),
        UserListQuery.Edge("xx43", UserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        UserListQuery.Edge("xx44", UserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("xx45", UserListQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page after
    val query3 = UserListQuery(Optional.Present(2), Optional.Present("xx45"))
    val data3 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx46", UserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        UserListQuery.Edge("xx47", UserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query3, data3)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx42", UserListQuery.Node("42", "John", "john@a.com", "User")),
        UserListQuery.Edge("xx43", UserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        UserListQuery.Edge("xx44", UserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("xx45", UserListQuery.Node("45", "Alice", "alice@a.com", "User")),
        UserListQuery.Edge("xx46", UserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        UserListQuery.Edge("xx47", UserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = UserListQuery(Optional.Absent, Optional.Absent, Optional.Present(2), Optional.Present("xx42"))
    val data4 = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx40", UserListQuery.Node("40", "Paul", "paul@a.com", "User")),
        UserListQuery.Edge("xx41", UserListQuery.Node("41", "Mary", "mary@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query4, data4)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = UserListQuery.Data(UserListQuery.Users(listOf(
        UserListQuery.Edge("xx40", UserListQuery.Node("40", "Paul", "paul@a.com", "User")),
        UserListQuery.Edge("xx41", UserListQuery.Node("41", "Mary", "mary@a.com", "User")),
        UserListQuery.Edge("xx42", UserListQuery.Node("42", "John", "john@a.com", "User")),
        UserListQuery.Edge("xx43", UserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        UserListQuery.Edge("xx44", UserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        UserListQuery.Edge("xx45", UserListQuery.Node("45", "Alice", "alice@a.com", "User")),
        UserListQuery.Edge("xx46", UserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        UserListQuery.Edge("xx47", UserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    client.apolloStore.accessCache { cache ->
      val record = cache.loadRecord("users", CacheHeaders.NONE)
      assertEquals(mapOf("startCursor" to "xx40", "endCursor" to "xx47"), record!!.metadata)
    }
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

@Suppress("UNCHECKED_CAST")
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

@Suppress("UNCHECKED_CAST")
class CursorPaginationRecordMerger : RecordMerger {
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
