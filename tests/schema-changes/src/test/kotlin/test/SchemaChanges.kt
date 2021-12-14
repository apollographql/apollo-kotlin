package test

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import org.junit.Test
import schema.changes.GetFieldQuery

class SchemaChangesTest {
  @Test
  fun schemaChanges() = runTest {
    val operation = GetFieldQuery()

    val v1Data = """
      {
        "field": {
          "__typename": "DefaultField",
          "id": "1",
          "name": "Name1"
        }
      }
    """.trimIndent()

    val v2Data = """
      {
        "field": {
          "__typename": "NewField",
          "id": "1",
          "name": "Name1"
        }
      }
    """.trimIndent()

    val data = operation.adapter().fromJson(
        Buffer().writeUtf8(v2Data).jsonReader(),
        CustomScalarAdapters.Empty
    )

    operation.normalize(data,
        customScalarAdapters = CustomScalarAdapters.Empty,
        cacheKeyGenerator = TypePolicyCacheKeyGenerator,
    )
  }
}
