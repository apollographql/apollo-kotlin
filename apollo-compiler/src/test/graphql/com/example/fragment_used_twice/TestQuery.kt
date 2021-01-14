// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_used_twice

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.example.fragment_used_twice.adapter.TestQuery_ResponseAdapter
import com.example.fragment_used_twice.fragment.HeroDetails
import com.example.fragment_used_twice.fragment.HumanDetails
import kotlin.Any
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery : Query<TestQuery.Data> {
  override fun operationId(): String = OPERATION_ID

  override fun queryDocument(): String = QUERY_DOCUMENT

  override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES

  override fun name(): String = OPERATION_NAME

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

      interface Character : Hero, HeroDetails {
        override val __typename: String

        /**
         * The name of the character
         */
        override val name: String

        interface Character : Hero.Character, HeroDetails.Character {
          override val __typename: String

          /**
           * The name of the character
           */
          override val name: String

          /**
           * The date character was born.
           */
          override val birthDate: Any
        }
      }

      interface Human : Hero, HumanDetails {
        override val __typename: String

        /**
         * What this human calls themselves
         */
        override val name: String

        interface Character : Human, HumanDetails.Character {
          override val __typename: String

          /**
           * What this human calls themselves
           */
          override val name: String

          /**
           * The date character was born.
           */
          override val birthDate: Any
        }
      }

      data class CharacterHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * The date character was born.
         */
        override val birthDate: Any
      ) : Hero, Character, HeroDetails, Character.Character, HeroDetails.Character

      data class CharacterHumanHero(
        override val __typename: String,
        /**
         * The name of the character
         */
        override val name: String,
        /**
         * The date character was born.
         */
        override val birthDate: Any
      ) : Hero, Character, HeroDetails, Character.Character, HeroDetails.Character, Human,
          HumanDetails, Human.Character, HumanDetails.Character

      data class OtherHero(
        override val __typename: String
      ) : Hero

      companion object {
        fun Hero.asCharacter(): Character? = this as? Character

        fun Hero.heroDetails(): HeroDetails? = this as? HeroDetails

        fun Hero.asHuman(): Human? = this as? Human

        fun Hero.humanDetails(): HumanDetails? = this as? HumanDetails
      }
    }
  }

  companion object {
    const val OPERATION_ID: String =
        "ef8624a3948f8b26cb3d63332d7d35e84a12896b20d91ecfcdb29c4825e8ff82"

    val QUERY_DOCUMENT: String = QueryDocumentMinifier.minify(
          """
          |query TestQuery {
          |  hero {
          |    __typename
          |    ...HeroDetails
          |    ...HumanDetails
          |  }
          |}
          |fragment HeroDetails on Character {
          |  __typename
          |  name
          |  ...CharacterDetails
          |}
          |fragment CharacterDetails on Character {
          |  __typename
          |  name
          |  birthDate
          |}
          |fragment HumanDetails on Human {
          |  __typename
          |  name
          |  ...CharacterDetails
          |}
          """.trimMargin()
        )

    val OPERATION_NAME: String = "TestQuery"
  }
}
