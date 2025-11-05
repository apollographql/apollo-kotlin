package test

import com.apollographql.apollo.api.FieldResult
import com.apollographql.apollo.api.graphQLErrorOrNull
import fragments.GetFooQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FragmentTest {
  @Test
  fun test() {
    // language=json
    val json = """
      {
        "errors": [
          { "message":  "Cannot return null for bar", "path": ["foo"] }        
        ],
        "data": {
          "__typename": "Foo",
          "foo": null        
        }
      }
    """.trimIndent()

    val response = GetFooQuery().parseResponse(json)
    response.data?.queryDetails?.foo.apply {
      assertIs<FieldResult.Failure>(this)
      assertEquals("Cannot return null for bar", this.graphQLErrorOrNull()?.message)
    }
  }
}