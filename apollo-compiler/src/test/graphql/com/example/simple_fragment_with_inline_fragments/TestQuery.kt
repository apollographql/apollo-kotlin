// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.simple_fragment_with_inline_fragments

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.simple_fragment_with_inline_fragments.adapter.TestQuery_ResponseAdapter
import com.example.simple_fragment_with_inline_fragments.fragment.HeroDetail
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery : Query<TestQuery.Data, Operation.Variables> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): OperationName = OPERATION_NAME

  override fun adapter(): ResponseAdapter<Data> = TestQuery_ResponseAdapter
  /**
   * The query type, represents all of the entry points into our object graph
   */
  data class Data(
    val hero: Hero?
  ) : Operation.Data {
    /**
     * A character from the Star Wars universe
     */
    interface Hero {
      val __typename: String

      interface Character : Hero, HeroDetail {
        override val __typename: String

        /**
         * The name of the character
         */
        override val name: String

        /**
         * The friends of the character, or an empty list if they have none
         */
        override val friends: List<Friend?>?

        /**
         * A character from the Star Wars universe
         */
        interface Friend : HeroDetail.Friend {
          override val __typename: String

          /**
           * The name of the character
           */
          override val name: String

          interface Human : Friend, HeroDetail.Friend.Human, HeroDetail.Friend {
            override val __typename: String

            /**
             * The name of the character
             */
            override val name: String

            /**
             * Height in the preferred unit, default is meters
             */
            override val height: Double?
          }

          interface Droid : Friend, HeroDetail.Friend.Droid, HeroDetail.Friend {
            override val __typename: String

            /**
             * The name of the character
             */
            override val name: String

            /**
             * This droid's primary function
             */
            override val primaryFunction: String?
          }

          companion object {
            fun Friend.asHuman(): Human? = this as? Human

            fun Friend.asDroid(): Droid? = this as? Droid
          }
        }
      }

      data class CharacterHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * The friends of the character, or an empty list if they have none
         */
        override val friends: List<Friend?>?
      ) : Hero, Character, HeroDetail {
        /**
         * A character from the Star Wars universe
         */
        interface Friend : Character.Friend, HeroDetail.Friend {
          override val __typename: String

<<<<<<< HEAD
          override fun marshaller(): ResponseFieldMarshaller
=======
          /**
           * The name of the character
           */
          override val name: String

          interface Human : Character.Friend, Character.Friend.Human, HeroDetail.Friend.Human,
              HeroDetail.Friend, Friend {
            override val __typename: String

            /**
             * The name of the character
             */
            override val name: String

            /**
             * Height in the preferred unit, default is meters
             */
            override val height: Double?
          }

          interface Droid : Character.Friend, Character.Friend.Droid, HeroDetail.Friend.Droid,
              HeroDetail.Friend, Friend {
            override val __typename: String

            /**
             * The name of the character
             */
            override val name: String

            /**
             * This droid's primary function
             */
            override val primaryFunction: String?
          }
>>>>>>> 7fb58f43... remove ResponseFieldMapper

          data class HumanFriend(
            override val __typename: String,
            /**
             * The name of the character
             */
            override val name: String,
            /**
             * Height in the preferred unit, default is meters
             */
            override val height: Double?
          ) : Character.Friend, Character.Friend.Human, HeroDetail.Friend.Human, HeroDetail.Friend,
<<<<<<< HEAD
              Friend {
            override fun marshaller(): ResponseFieldMarshaller {
              return ResponseFieldMarshaller { writer ->
                TestQuery_ResponseAdapter.Data.Hero.CharacterHero.Friend.HumanFriend.toResponse(writer, this)
              }
            }
          }
=======
              Friend, Human
>>>>>>> 7fb58f43... remove ResponseFieldMapper

          data class DroidFriend(
            override val __typename: String,
            /**
             * The name of the character
             */
            override val name: String,
            /**
             * This droid's primary function
             */
            override val primaryFunction: String?
          ) : Character.Friend, Character.Friend.Droid, HeroDetail.Friend.Droid, HeroDetail.Friend,
<<<<<<< HEAD
              Friend {
            override fun marshaller(): ResponseFieldMarshaller {
              return ResponseFieldMarshaller { writer ->
                TestQuery_ResponseAdapter.Data.Hero.CharacterHero.Friend.DroidFriend.toResponse(writer, this)
              }
            }
          }
=======
              Friend, Droid
>>>>>>> 7fb58f43... remove ResponseFieldMapper

          data class OtherFriend(
            override val __typename: String,
            /**
             * The name of the character
             */
            override val name: String
<<<<<<< HEAD
          ) : Character.Friend, HeroDetail.Friend, Friend {
            override fun marshaller(): ResponseFieldMarshaller {
              return ResponseFieldMarshaller { writer ->
                TestQuery_ResponseAdapter.Data.Hero.CharacterHero.Friend.OtherFriend.toResponse(writer, this)
              }
            }
=======
          ) : Character.Friend, HeroDetail.Friend, Friend

          companion object {
            fun Friend.asFriends(): HeroDetail.Friend? = this as? HeroDetail.Friend

            fun Friend.asHuman(): Human? = this as? Human

            fun Friend.asDroid(): Droid? = this as? Droid
>>>>>>> 7fb58f43... remove ResponseFieldMapper
          }
        }
      }

      data class OtherHero(
        override val __typename: String
      ) : Hero

      companion object {
        fun Hero.asCharacter(): Character? = this as? Character

        fun Hero.heroDetails(): HeroDetail? = this as? HeroDetail
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "d5cb325c69027e05bed9d8cbfa589f8c521df26be01c87bf9169d7c931f04489"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    ...HeroDetails
          |  }
          |}
          |fragment HeroDetails on Character {
          |  __typename
          |  name
          |  friends {
          |    __typename
          |    name
          |    ... on Human {
          |      height
          |    }
          |    ... on Droid {
          |      primaryFunction
          |    }
          |  }
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: OperationName = object : OperationName {
      override fun name(): String {
        return "TestQuery"
      }
    }
  }
}
