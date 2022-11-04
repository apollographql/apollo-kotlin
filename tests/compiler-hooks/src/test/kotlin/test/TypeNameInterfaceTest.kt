package test

import hooks.typenameinterface.HasTypeName
import hooks.typenameinterface.NodeQuery
import hooks.typenameinterface.UserQuery
import hooks.typenameinterface.fragment.UserFragment
import org.junit.Test

class TypeNameInterfaceTest {
  @Test
  fun typeNameInterface() {
    val node: HasTypeName = NodeQuery.Node("", "", null)
    val user: HasTypeName = UserQuery.User("", "", "", UserFragment(null))
  }
}
