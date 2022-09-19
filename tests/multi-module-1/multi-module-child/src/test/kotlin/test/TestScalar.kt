package test

import multimodule.child.GetLong2Query
import multimodule.root.fragment.QueryDetails
import org.junit.Test
import kotlin.test.assertIs

class TestScalar {
  @Test
  fun testScalar() {
    val data = GetLong2Query.Data("", 0, QueryDetails(0))
    assertIs<Long>(data.long2)
  }
}
