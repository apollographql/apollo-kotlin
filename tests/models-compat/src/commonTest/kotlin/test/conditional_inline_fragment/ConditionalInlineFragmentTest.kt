package test.conditional_inline_fragment

import conditional_inline_fragment.GetFieldQuery
import kotlin.test.Test

class ConditionalInlineFragmentTest {
  @Test
  fun test() {
    /**
     * This test makes sure that the inline fragment is not merged since it has an include condition
     * Just compiling is enough, so it could in theory be a compiler test but they are so big that it would
     * be easy to miss a change in the class structure and parameters
     */
    GetFieldQuery.Data(
        __typename = "Query",
        field1 = "banana",
        asQuery = GetFieldQuery.AsQuery(
            __typename = "Query",
            field1 = "banana",
            field2 = "croissant"
        )
    )
  }
}