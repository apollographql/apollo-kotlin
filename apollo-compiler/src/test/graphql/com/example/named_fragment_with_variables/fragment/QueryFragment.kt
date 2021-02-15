// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.named_fragment_with_variables.fragment

import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface QueryFragment {
  val __typename: String

  val organization: Organization?

  interface Organization {
    val id: String

    val user: List<User>

    interface User {
      val __typename: String

      interface User : Organization.User, UserFragment

      companion object {
        fun Organization.User.asUser(): User? = this as? User

        fun Organization.User.userFragment(): UserFragment? = this as? UserFragment
      }
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment QueryFragment on Query {
        |  __typename
        |  organization(id: ${'$'}organizationId) {
        |    id
        |    user(query: ${'$'}query) {
        |      __typename
        |      ...UserFragment
        |    }
        |  }
        |}
        """.trimMargin()
  }
}
