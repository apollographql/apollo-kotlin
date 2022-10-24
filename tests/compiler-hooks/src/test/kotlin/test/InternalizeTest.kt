package test

import hooks.internalize.NodeQuery
import hooks.internalize.UserQuery
import org.junit.Test
import kotlin.reflect.KVisibility
import kotlin.test.assertEquals

class InternalizeTest {
  @Test
  fun internalize() {
    assertEquals(KVisibility.PUBLIC, UserQuery::class.visibility)
    assertEquals(KVisibility.INTERNAL, NodeQuery::class.visibility)
  }
}
