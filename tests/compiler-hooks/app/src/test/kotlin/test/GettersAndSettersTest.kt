package test

import hooks.gettersandsetters.NodeQuery
import org.junit.Test

class GettersAndSettersTest {
  @Test
  fun gettersAndSetters() {
    val node = NodeQuery.Node("", "", null)
    node.setTypename("typename")
    node.setId("id")
    node.setOnUser(null)
  }
}
