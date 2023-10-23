package pagination

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.DefaultFieldNameGenerator
import com.apollographql.apollo3.cache.normalized.api.FieldNameContext
import com.apollographql.apollo3.cache.normalized.api.FieldNameGenerator
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.FieldRecordMerger
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.internal.runTest
import pagination.type.buildUser
import pagination.type.buildUserPage
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetBasedWithPageAndInputPaginationTest {
  @Test
  fun offsetBasedWithPageAndInputMemoryCache() {
    offsetBasedWithPageAndInput(MemoryCacheFactory())
  }

  @Test
  fun offsetBasedWithPageAndInputBlobSqlCache() {
    offsetBasedWithPageAndInput(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun offsetBasedWithPageAndInputJsonSqlCache() {
    offsetBasedWithPageAndInput(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  @Test
  fun offsetBasedWithPageAndInputChainedCache() {
    offsetBasedWithPageAndInput(MemoryCacheFactory().chain(SqlNormalizedCacheFactory(name = "json", withDates = false)))
  }

  private fun offsetBasedWithPageAndInput(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = OffsetPaginationMetadataGenerator("UserPage"),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = FieldRecordMerger(OffsetPaginationFieldMerger()),
        fieldNameGenerator = UsersNameGenerator,
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(42), limit = Optional.Present(2))
    val data1 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "42" },
            buildUser { id = "43" },
        )
      }
    }
    apolloStore.writeOperation(query1, data1)
    var dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page after
    val query2 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(44), limit = Optional.Present(2))
    val data2 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "44" },
            buildUser { id = "45" },
        )
      }
    }
    apolloStore.writeOperation(query2, data2)
    dataFromStore = apolloStore.readOperation(query1)
    var expectedData = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "42" },
            buildUser { id = "43" },
            buildUser { id = "44" },
            buildUser { id = "45" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page in the middle
    val query3 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(44), limit = Optional.Present(3))
    val data3 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "44" },
            buildUser { id = "45" },
            buildUser { id = "46" },
        )
      }
    }
    apolloStore.writeOperation(query3, data3)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "42" },
            buildUser { id = "43" },
            buildUser { id = "44" },
            buildUser { id = "45" },
            buildUser { id = "46" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Page before
    val query4 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(40), limit = Optional.Present(2))
    val data4 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "40" },
            buildUser { id = "41" },
        )
      }
    }
    apolloStore.writeOperation(query4, data4)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "40" },
            buildUser { id = "41" },
            buildUser { id = "42" },
            buildUser { id = "43" },
            buildUser { id = "44" },
            buildUser { id = "45" },
            buildUser { id = "46" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Non-contiguous page (should reset)
    val query5 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(50), limit = Optional.Present(2))
    val data5 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = listOf(
            buildUser { id = "50" },
            buildUser { id = "51" },
        )
      }
    }
    apolloStore.writeOperation(query5, data5)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)

    // Empty page (should keep previous result)
    val query6 = UsersOffsetBasedWithPageAndInputQuery(offset = Optional.Present(52), limit = Optional.Present(2))
    val data6 = UsersOffsetBasedWithPageAndInputQuery.Data {
      usersOffsetBasedWithPage = buildUserPage {
        users = emptyList()
      }
    }
    apolloStore.writeOperation(query6, data6)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
    assertChainedCachesAreEqual(apolloStore)
  }

  private class OffsetPaginationMetadataGenerator(private val typeName: String) : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.type.rawType().name == typeName) {
        return mapOf("offset" to context.argumentValue("offset"))
      }
      return emptyMap()
    }
  }

  private class OffsetPaginationFieldMerger : FieldRecordMerger.FieldMerger {
    override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
      val existingOffset = existing.metadata["offset"] as? Int
      val incomingOffset = incoming.metadata["offset"] as? Int
      return if (existingOffset == null || incomingOffset == null) {
        incoming
      } else {
        val existingValue = existing.value as Map<*, *>
        val existingList = existingValue["users"] as List<*>
        val incomingList = (incoming.value as Map<*, *>)["users"] as List<*>
        val (mergedList, mergedOffset) = mergeLists(existingList, incomingList, existingOffset, incomingOffset)
        val mergedFieldValue = existingValue.toMutableMap()
        mergedFieldValue["users"] = mergedList
        FieldRecordMerger.FieldInfo(
            value = mergedFieldValue,
            metadata = mapOf("offset" to mergedOffset)
        )
      }
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

  object UsersNameGenerator : FieldNameGenerator {
    override fun getFieldName(context: FieldNameContext): String {
      return if (context.parentType == "Query" && context.field.name == "usersOffsetBasedWithPageAndInput") {
        context.field.nameWithoutPaginationArguments(context.variables)
      } else {
        DefaultFieldNameGenerator.getFieldName(context)
      }
    }

    private fun CompiledField.nameWithoutPaginationArguments(variables: Executable.Variables): String {
      val filteredArguments = arguments.map {
        if (it.name == "usersInput") {
          CompiledArgument.Builder(it.name, (it.value as Map<*, *>).filterKeys { it != "offset" && it != "limit" }).build()
        } else {
          it
        }
      }
      return CompiledField.Builder(this)
          .arguments(filteredArguments)
          .build()
          .nameWithArguments(variables)
    }
  }
}
