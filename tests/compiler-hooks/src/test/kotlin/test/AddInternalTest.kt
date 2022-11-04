package test

import hooks.addinternal.NodeQuery
import hooks.addinternal.UserQuery
import org.junit.Test
import kotlin.reflect.KVisibility
import kotlin.test.assertEquals

class AddInternalTest {
  @Test
  fun addInternal() {
    assertEquals(KVisibility.PUBLIC, UserQuery::class.visibility)
    assertEquals(KVisibility.INTERNAL, NodeQuery::class.visibility)
  }
}
