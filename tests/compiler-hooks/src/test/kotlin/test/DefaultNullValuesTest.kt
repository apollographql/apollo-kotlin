package test

import hooks.defaultnullvalues.NodeQuery
import org.junit.Test
import kotlin.test.assertNull

class DefaultNullValuesTest {
  @Test
  fun defaultNullValues() {
    val nodeData = NodeQuery.Data()
    assertNull(nodeData.node)

    val onUser = NodeQuery.OnUser("John")
    assertNull(onUser.job)
  }
}
