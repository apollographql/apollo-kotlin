package pagination

import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.FieldRecordMerger
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.testing.runTest
import pagination.test.UsersOffsetBasedWithPageQuery_TestBuilder.Data
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals


class OffsetBasedWithPagePaginationTest {
  @Test
  fun offsetBasedWithPageMemoryCache() {
    offsetBasedWithPage(MemoryCacheFactory())
  }

  @Test
  fun offsetBasedWithPageBlobSqlCache() {
    offsetBasedWithPage(SqlNormalizedCacheFactory(name = "blob", withDates = true))
  }

  @Test
  fun offsetBasedWithPageJsonSqlCache() {
    offsetBasedWithPage(SqlNormalizedCacheFactory(name = "json", withDates = false))
  }

  private fun offsetBasedWithPage(cacheFactory: NormalizedCacheFactory) = runTest {
    val apolloStore = ApolloStore(
        normalizedCacheFactory = cacheFactory,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
        metadataGenerator = OffsetPaginationMetadataGenerator("UserPage"),
        apolloResolver = FieldPolicyApolloResolver,
        recordMerger = FieldRecordMerger(OffsetPaginationRecordMerger())
    )
    apolloStore.clearAll()

    // First page
    val query1 = UsersOffsetBasedWithPageQuery(offset = Optional.Present(42), limit = Optional.Present(2))
    val data1 = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "42" },
            user { id = "43" },
        )
      }
    }
    apolloStore.writeOperation(query1, data1)
    var dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data1, dataFromStore)

    // Page after
    val query2 = UsersOffsetBasedWithPageQuery(offset = Optional.Present(44), limit = Optional.Present(2))
    val data2 = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "44" },
            user { id = "45" },
        )
      }
    }
    apolloStore.writeOperation(query2, data2)
    dataFromStore = apolloStore.readOperation(query1)
    var expectedData = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "42" },
            user { id = "43" },
            user { id = "44" },
            user { id = "45" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)

    // Page in the middle
    val query3 = UsersOffsetBasedWithPageQuery(offset = Optional.Present(44), limit = Optional.Present(3))
    val data3 = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "44" },
            user { id = "45" },
            user { id = "46" },
        )
      }
    }
    apolloStore.writeOperation(query3, data3)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "42" },
            user { id = "43" },
            user { id = "44" },
            user { id = "45" },
            user { id = "46" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)

    // Page before
    val query4 = UsersOffsetBasedWithPageQuery(offset = Optional.Present(40), limit = Optional.Present(2))
    val data4 = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "40" },
            user { id = "41" },
        )
      }
    }
    apolloStore.writeOperation(query4, data4)
    dataFromStore = apolloStore.readOperation(query1)
    expectedData = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "40" },
            user { id = "41" },
            user { id = "42" },
            user { id = "43" },
            user { id = "44" },
            user { id = "45" },
            user { id = "46" },
        )
      }
    }
    assertEquals(expectedData, dataFromStore)

    // Non-contiguous page (should reset)
    val query5 = UsersOffsetBasedWithPageQuery(offset = Optional.Present(50), limit = Optional.Present(2))
    val data5 = UsersOffsetBasedWithPageQuery.Data {
      usersOffsetBasedWithPage = usersOffsetBasedWithPage {
        users = listOf(
            user { id = "50" },
            user { id = "51" },
        )
      }
    }
    apolloStore.writeOperation(query5, data5)
    dataFromStore = apolloStore.readOperation(query1)
    assertEquals(data5, dataFromStore)
  }

  private class OffsetPaginationMetadataGenerator(private val typeName: String) : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
      if (context.field.type.leafType().name == typeName) {
        return mapOf("offset" to context.argumentValue("offset"))
      }
      return emptyMap()
    }
  }

  private class OffsetPaginationRecordMerger : FieldRecordMerger.FieldMerger {
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
}
