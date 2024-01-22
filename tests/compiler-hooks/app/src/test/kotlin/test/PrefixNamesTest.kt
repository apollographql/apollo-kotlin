package test

import hooks.prefixnames.kotlin.GQLNodeQuery
import hooks.prefixnames.kotlin.type.GQLJob
import org.junit.Test

@Suppress("UNUSED_VARIABLE")
class PrefixNamesTest {
  @Test
  fun kotlinPrefixNames() {
    val nodeQuery: GQLNodeQuery = GQLNodeQuery()
    val node: GQLNodeQuery.Node = GQLNodeQuery.Node("", "", null)
    val onUser: GQLNodeQuery.OnUser = GQLNodeQuery.OnUser("John", GQLJob.engineer)
  }

  @Test
  fun javaPrefixNames() {
    val nodeQuery: hooks.prefixnames.java.GQLNodeQuery = hooks.prefixnames.java.GQLNodeQuery()
    val node: hooks.prefixnames.java.GQLNodeQuery.Node = hooks.prefixnames.java.GQLNodeQuery.Node("", "", null)
    val onUser: hooks.prefixnames.java.GQLNodeQuery.OnUser = hooks.prefixnames.java.GQLNodeQuery.OnUser("John", hooks.prefixnames.java.type.GQLJob.engineer)
  }
}
