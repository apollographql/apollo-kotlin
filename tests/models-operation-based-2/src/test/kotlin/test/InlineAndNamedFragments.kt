package test

import codegen.models.InlineAndNamedFragmentQuery
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.testing.internal.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertTrue

class InlineAndNamedFragments {
  @Test
  fun canReadInlineAndNamedFragments() = runTest {
    val dataString = """
      {
        "hero": {
          "__typename": "Droid",
          "primaryFunction": "translation"
        }
      }
    """.trimIndent()

    val data = InlineAndNamedFragmentQuery().adapter().fromJson(Buffer().apply { writeUtf8(dataString) }.jsonReader(), CustomScalarAdapters.Empty)
    assertTrue(data.hero?.onDroid?.primaryFunction == "translation")
  }
}
