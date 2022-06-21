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
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals

class CursorPaginationTest {
  @Test
  fun cursorBasedMemoryCache() {
    cursorBased(MemoryCacheFactory())
  }

  @Test
  fun cursorBasedBlobSqlCache() {
    cursorBased(SqlNormalizedCacheFactory(name = null, withDates = true))
  }

  @Test
  fun cursorBasedJsonSqlCache() {
    cursorBased(SqlNormalizedCacheFactory(name = null, withDates = false))
  }

  @Test
  fun offsetBasedMemoryCache() {
    offsetBased(MemoryCacheFactory())
  }

  @Test
  fun offsetBasedBlobSqlCache() {
    offsetBased(SqlNormalizedCacheFactory(name = null, withDates = true))
  }

  @Test
  fun offsetBasedJsonSqlCache() {
    offsetBased(SqlNormalizedCacheFactory(name = null, withDates = false))
  }


  private fun cursorBased(cacheFactory: NormalizedCacheFactory) = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = cacheFactory,
            cacheKeyGenerator = IgnoreArgumentsKeyGenerator("UserConnection"),
            metadataGenerator = CursorPaginationMetadataGenerator(),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = CursorPaginationRecordMerger()
        )
        .serverUrl("unused")
        .build()

    // First page
    val query1 = CursorBasedUserListQuery(Optional.Present(2))
    val data1 = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx42", CursorBasedUserListQuery.Node("42", "John", "john@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx43", CursorBasedUserListQuery.Node("43", "Jane", "jane@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query1, data1)
    var dataFromStore = client.apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = CursorBasedUserListQuery(Optional.Present(2), Optional.Present("xx43"))
    val data2 = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx44", CursorBasedUserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx45", CursorBasedUserListQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query2, data2)
    dataFromStore = client.apolloStore.readOperation(query1)
    var expectedData = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx42", CursorBasedUserListQuery.Node("42", "John", "john@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx43", CursorBasedUserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx44", CursorBasedUserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx45", CursorBasedUserListQuery.Node("45", "Alice", "alice@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page after
    val query3 = CursorBasedUserListQuery(Optional.Present(2), Optional.Present("xx45"))
    val data3 = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx46", CursorBasedUserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx47", CursorBasedUserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query3, data3)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx42", CursorBasedUserListQuery.Node("42", "John", "john@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx43", CursorBasedUserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx44", CursorBasedUserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx45", CursorBasedUserListQuery.Node("45", "Alice", "alice@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx46", CursorBasedUserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx47", CursorBasedUserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = CursorBasedUserListQuery(Optional.Absent, Optional.Absent, Optional.Present(2), Optional.Present("xx42"))
    val data4 = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx40", CursorBasedUserListQuery.Node("40", "Paul", "paul@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx41", CursorBasedUserListQuery.Node("41", "Mary", "mary@a.com", "User")),
    )))
    client.apolloStore.writeOperation(query4, data4)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = CursorBasedUserListQuery.Data(CursorBasedUserListQuery.UsersCursorBased(listOf(
        CursorBasedUserListQuery.Edge("xx40", CursorBasedUserListQuery.Node("40", "Paul", "paul@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx41", CursorBasedUserListQuery.Node("41", "Mary", "mary@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx42", CursorBasedUserListQuery.Node("42", "John", "john@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx43", CursorBasedUserListQuery.Node("43", "Jane", "jane@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx44", CursorBasedUserListQuery.Node("44", "Peter", "peter@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx45", CursorBasedUserListQuery.Node("45", "Alice", "alice@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx46", CursorBasedUserListQuery.Node("46", "Bob", "bob@a.com", "User")),
        CursorBasedUserListQuery.Edge("xx47", CursorBasedUserListQuery.Node("47", "Charlie", "charlie@a.com", "User")),
    )))
    assertEquals(expectedData, dataFromStore)

    client.apolloStore.accessCache { cache ->
      val record = cache.loadRecord("usersCursorBased", CacheHeaders.NONE)
      assertEquals(mapOf("startCursor" to "xx40", "endCursor" to "xx47"), record!!.metadata)
    }
  }

  private fun offsetBased(cacheFactory: NormalizedCacheFactory) = runTest {
    val client = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = cacheFactory,
            cacheKeyGenerator = IgnoreArgumentsKeyGenerator("UserPage"),
            metadataGenerator = OffsetPaginationMetadataGenerator("UserPage"),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = OffsetPaginationRecordMerger()
        )
        .serverUrl("unused")
        .build()

    // First page
    val query1 = OffsetBasedUserList2Query(Optional.Present(42), Optional.Present(2))
    val data1 = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("42", "John", "john@a.com", "User"),
        OffsetBasedUserList2Query.User("43", "Jane", "jane@a.com", "User"),
    )))
    client.apolloStore.writeOperation(query1, data1)
    var dataFromStore = client.apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = OffsetBasedUserList2Query(Optional.Present(44), Optional.Present(2))
    val data2 = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("44", "Peter", "peter@a.com", "User"),
        OffsetBasedUserList2Query.User("45", "Alice", "alice@a.com", "User"),
    )))
    client.apolloStore.writeOperation(query2, data2)
    dataFromStore = client.apolloStore.readOperation(query1)
    var expectedData = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("42", "John", "john@a.com", "User"),
        OffsetBasedUserList2Query.User("43", "Jane", "jane@a.com", "User"),
        OffsetBasedUserList2Query.User("44", "Peter", "peter@a.com", "User"),
        OffsetBasedUserList2Query.User("45", "Alice", "alice@a.com", "User"),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page in the middle
    val query3 = OffsetBasedUserList2Query(Optional.Present(44), Optional.Present(3))
    val data3 = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("44", "Peter", "peter@a.com", "User"),
        OffsetBasedUserList2Query.User("45", "Alice", "alice@a.com", "User"),
        OffsetBasedUserList2Query.User("46", "Bob", "bob@a.com", "User"),
    )))
    client.apolloStore.writeOperation(query3, data3)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("42", "John", "john@a.com", "User"),
        OffsetBasedUserList2Query.User("43", "Jane", "jane@a.com", "User"),
        OffsetBasedUserList2Query.User("44", "Peter", "peter@a.com", "User"),
        OffsetBasedUserList2Query.User("45", "Alice", "alice@a.com", "User"),
        OffsetBasedUserList2Query.User("46", "Bob", "bob@a.com", "User"),
    )))
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = OffsetBasedUserList2Query(Optional.Present(40), Optional.Present(2))
    val data4 = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("40", "Paul", "paul@a.com", "User"),
        OffsetBasedUserList2Query.User("41", "Mary", "mary@a.com", "User"),
    )))
    client.apolloStore.writeOperation(query4, data4)
    dataFromStore = client.apolloStore.readOperation(query1)
    expectedData = OffsetBasedUserList2Query.Data(OffsetBasedUserList2Query.UsersOffsetBased2(listOf(
        OffsetBasedUserList2Query.User("40", "Paul", "paul@a.com", "User"),
        OffsetBasedUserList2Query.User("41", "Mary", "mary@a.com", "User"),
        OffsetBasedUserList2Query.User("42", "John", "john@a.com", "User"),
        OffsetBasedUserList2Query.User("43", "Jane", "jane@a.com", "User"),
        OffsetBasedUserList2Query.User("44", "Peter", "peter@a.com", "User"),
        OffsetBasedUserList2Query.User("45", "Alice", "alice@a.com", "User"),
        OffsetBasedUserList2Query.User("46", "Bob", "bob@a.com", "User"),
    )))
    assertEquals(expectedData, dataFromStore)

    client.apolloStore.accessCache { cache ->
      val record = cache.loadRecord("usersOffsetBased2", CacheHeaders.NONE)
      assertEquals(mapOf("offset" to 40), record!!.metadata)
    }
  }
}

class IgnoreArgumentsKeyGenerator(private val typeName: String) : CacheKeyGenerator {
  override fun cacheKeyForObject(obj: Map<String, Any?>, context: CacheKeyGeneratorContext): CacheKey? {
    if (context.field.type.leafType().name == typeName) {
      return CacheKey(context.field.alias ?: context.field.name)
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

@Suppress("UNCHECKED_CAST")
class OffsetPaginationMetadataGenerator(private val typeName: String) : MetadataGenerator {
  override fun metadataForObject(obj: Map<String, Any?>, context: MetadataGeneratorContext): Map<String, Any?> {
    if (context.field.type.leafType().name == typeName) {
      return mapOf(
          "offset" to context.field.resolveArgument("offset", context.variables) as Int,
      )
    }
    return emptyMap()
  }
}

@Suppress("UNCHECKED_CAST")
class OffsetPaginationRecordMerger : RecordMerger {
  override fun merge(existing: Record, incoming: Record, newDate: Long?): Pair<Record, Set<String>> {
    val existingOffset = existing.metadata["offset"] as? Int
    val incomingOffset = incoming.arguments["offset"] as? Int
    if (existingOffset == null || incomingOffset == null) {
      return DefaultRecordMerger.merge(existing, incoming, newDate)
    }

    val existingUsers = existing["users"] as List<Record>
    val incomingUsers = incoming["users"] as List<Record>
    val mergedUsers: List<Record> = mergeLists(existingUsers, incomingUsers, existingOffset, incomingOffset)
    val mergedFields = mapOf(
        "users" to mergedUsers,
    )
    val changedKeys = setOf("users")
    val date = existing.date?.toMutableMap() ?: mutableMapOf()
    if (newDate != null) {
      date["users"] = newDate
    }
    val metadata = mapOf("offset" to min(existingOffset, incomingOffset))

    return Record(
        key = existing.key,
        fields = mergedFields,
        mutationId = incoming.mutationId,
        date = date,
        arguments = emptyMap(),
        metadata = metadata,
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
