package test

import hooks.prefixnames.GQLNodeQuery
import hooks.prefixnames.type.GQLJob
import org.junit.Test

class PrefixNamesTest {
  @Test
  fun prefixNames() {
    val nodeQuery: GQLNodeQuery = GQLNodeQuery()
    val node: GQLNodeQuery.Node = GQLNodeQuery.Node("", "", null)
    val onUser: GQLNodeQuery.OnUser = GQLNodeQuery.OnUser("John", GQLJob.engineer)
  }
}
