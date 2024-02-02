package test

import hooks.prefixnames.kotlin.GQLNodeQuery
import hooks.prefixnames.kotlin.type.GQLJob
import org.junit.Test

@Suppress("UNUSED_VARIABLE")
class PrefixNamesTest {
  @Test
  fun kotlinPrefixNames() {
    val nodeQuery = GQLNodeQuery()
    val node: GQLNodeQuery.Node = GQLNodeQuery.Node("", "", null)
    val onUser: GQLNodeQuery.OnUser = GQLNodeQuery.OnUser("John", GQLJob.engineer)
  }
}
