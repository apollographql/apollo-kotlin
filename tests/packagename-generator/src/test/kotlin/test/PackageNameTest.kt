package test

import foo_operation.GetFooQuery
import foo_schema.type.Query
import kotlin.test.Test

class PackageNameTest {
  @Test
  fun packageName() {
    /**
     * Does nothing, just ensures it compiles
     */
    Query
    GetFooQuery()
  }
}
