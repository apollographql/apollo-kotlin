package test

import nested.GetFQuery
import nested.GetFooQuery
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedTest {
  @Test
  fun parentFieldCatchesException() {
    val response = GetFooQuery().parseResponse("""
      {
        "errors": [{"message": "Oops", "path": ["foo", "bar"] }],
        "data": { "foo": { "bar": null } }
      }
    """.trimIndent())

    assertNotNull(response.data)
    assertNull(response.data?.foo)
  }

  @Test
  fun serverSendsNullWithoutError() {
    /**
     * Server sends back null without an error on a semantic non-null field
     */
    val response = GetFQuery().parseResponse("""
      {
        "data": { "foo": { "f": null } }
      }
    """.trimIndent())

    assertNull(response.data)
    assertNotNull(response.exception)
    assertTrue(response.exception!!.message!!.contains("Expected a name but was NULL at path data.foo.f"))
  }
}
