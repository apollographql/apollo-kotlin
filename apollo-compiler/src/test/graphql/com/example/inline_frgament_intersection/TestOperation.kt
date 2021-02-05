// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.inline_frgament_intersection

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.inline_frgament_intersection.adapter.TestOperation_ResponseAdapter
import com.example.inline_frgament_intersection.type.Race
import kotlin.Boolean
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestOperation : Query<TestOperation.Data> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): String = OPERATION_NAME

  override fun adapter(): ResponseAdapter<Data> = TestOperation_ResponseAdapter
  override fun responseFields(): List<ResponseField.FieldSet> = listOf(
    ResponseField.FieldSet(null, TestOperation_ResponseAdapter.RESPONSE_FIELDS)
  )
  data class Data(
    val random: Random
  ) : Operation.Data {
    interface Random {
      val __typename: String

      interface Being : Random {
        override val __typename: String

        val name: String

        val friends: List<Friends>

        interface Friends {
          val __typename: String

          val name: String

          interface Wookie : Friends {
            override val __typename: String

            override val name: String

            val lifeExpectancy: Double?
          }

          companion object {
            fun Friends.asWookie(): Wookie? = this as? Wookie
          }
        }

        interface Human : Being {
          override val __typename: String

          override val name: String

          override val friends: List<Friends>

          val profilePictureUrl: String?

          interface Friends : Being.Friends {
            override val __typename: String

            override val name: String

            val isFamous: Boolean?

            interface Wookie : Being.Friends, Being.Friends.Wookie, Friends {
              override val __typename: String

              override val name: String

              override val lifeExpectancy: Double?

              override val isFamous: Boolean?

              val race: Race
            }

            companion object {
              fun Friends.asWookie(): Wookie? = this as? Wookie
            }
          }
        }
      }

      interface Wookie : Random {
        override val __typename: String

        val race: Race

        val friends: List<Friends>

        interface Friends {
          val lifeExpectancy: Double?
        }
      }

      data class BeingHumanRandom(
        override val __typename: String,
        override val name: String,
        override val friends: List<Friends>,
        override val profilePictureUrl: String?
      ) : Random, Being, Being.Human {
        interface Friends : Being.Friends, Being.Human.Friends {
          override val __typename: String

          data class WookieFriends(
            override val __typename: String,
            override val name: String,
            override val isFamous: Boolean?,
            override val lifeExpectancy: Double?,
            override val race: Race
          ) : Being.Friends, Being.Friends.Wookie, Being.Human.Friends, Being.Human.Friends.Wookie,
              Friends

          data class OtherFriends(
            override val __typename: String,
            override val name: String,
            override val isFamous: Boolean?
          ) : Being.Friends, Being.Human.Friends, Friends
        }
      }

      data class BeingWookieRandom(
        override val __typename: String,
        override val name: String,
        override val friends: List<Friends>,
        override val race: Race
      ) : Random, Being, Wookie {
        interface Friends : Being.Friends, Wookie.Friends {
          override val __typename: String

          data class WookieFriends(
            override val __typename: String,
            override val name: String,
            override val lifeExpectancy: Double?
          ) : Being.Friends, Being.Friends.Wookie, Friends

          data class OtherFriends(
            override val __typename: String,
            override val name: String,
            override val lifeExpectancy: Double?
          ) : Being.Friends, Wookie.Friends, Friends
        }
      }

      data class OtherRandom(
        override val __typename: String
      ) : Random

      companion object {
        fun Random.asBeing(): Being? = this as? Being

        fun Random.asWookie(): Wookie? = this as? Wookie
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "c99acd467295cdc6cf49f2f0e260f5879de276fba742d6faa161453e53fbf9d4"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestOperation {
          |  random {
          |    __typename
          |    ... on Being {
          |      __typename
          |      name
          |      friends {
          |        __typename
          |        name
          |        ... on Wookie {
          |          lifeExpectancy
          |        }
          |      }
          |      ... on Human {
          |        profilePictureUrl
          |        friends {
          |          __typename
          |          isFamous
          |          ... on Wookie {
          |            race
          |          }
          |        }
          |      }
          |    }
          |    ... on Wookie {
          |      race
          |      friends {
          |        lifeExpectancy
          |      }
          |    }
          |    ... on Being {
          |      friends {
          |        __typename
          |        ... on Wookie {
          |          lifeExpectancy
          |        }
          |      }
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestOperation"
  }
}
