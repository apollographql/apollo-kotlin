package test.nonnull

import com.apollographql.apollo3.api.parseData
import com.apollographql.apollo3.integration.nonnull.NonNullField1Query
import com.apollographql.apollo3.integration.nonnull.NonNullField2Query
import com.apollographql.apollo3.integration.nonnull.NullableField1Query
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.fail

class NonNullTest {
  private val field1Response = """
      {
        "field1": null
      }
    """.trimIndent()

  private val field2Response = """
      {
        "field2": null
      }
    """.trimIndent()

  @Test
  fun failsWithAnnotationInQuery() {
    try {
      NonNullField1Query().parseData(field1Response)
      fail("An exception was expected")
    } catch (e: Exception) {
      check(e.message?.contains("but was NULL at path field1") == true)
    }
  }

  @Test
  fun failsWithAnnotationInSchema() {
    try {
      NonNullField2Query().parseData(field2Response)
      fail("An exception was expected")
    } catch (e: Exception) {
      check(e.message?.contains("but was NULL at path field2") == true)
    }
  }

  @Test
  fun succeedsWithoutAnnotationInQuery() {
    val data = NullableField1Query().parseData(field1Response)
    assertEquals(null, data.field1)
  }

  @Test
  fun queryDocumentDoesNotContainNonNull() {
    assertFalse(NonNullField1Query.OPERATION_DOCUMENT.contains("nonnull"))
    assertFalse(NullableField1Query.OPERATION_DOCUMENT.contains("nonnull"))
  }
}