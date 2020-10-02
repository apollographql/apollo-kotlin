// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_in_fragment.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import kotlin.Array
import kotlin.String
import kotlin.Suppress

/**
 * An individual person or character within the Star Wars universe.
 */
@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface PilotFragment : GraphqlFragment {
  val __typename: String

  /**
   * The name of this person.
   */
  val name: String?

  /**
   * A planet that this person was born on or inhabits.
   */
  val homeworld: Homeworld?

  /**
   * A large mass, planet or planetoid in the Star Wars Universe, at the time of
   * 0 ABY.
   */
  interface Homeworld {
    val __typename: String

    fun marshaller(): ResponseFieldMarshaller
  }

  /**
   * A large mass, planet or planetoid in the Star Wars Universe, at the time of
   * 0 ABY.
   */
  data class Homeworld1(
    override val __typename: String = "Planet",
    /**
     * The name of this planet.
     */
    override val name: String?
  ) : PlanetFragment, Homeworld {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@Homeworld1.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@Homeworld1.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): Homeworld1 = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])
        Homeworld1(
          __typename = __typename,
          name = name
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<Homeworld1> = ResponseFieldMapper { invoke(it) }
    }
  }

  /**
   * An individual person or character within the Star Wars universe.
   */
  data class DefaultImpl(
    override val __typename: String = "Person",
    /**
     * The name of this person.
     */
    override val name: String?,
    /**
     * A planet that this person was born on or inhabits.
     */
    override val homeworld: Homeworld1?
  ) : PilotFragment {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@DefaultImpl.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@DefaultImpl.name)
        writer.writeObject(RESPONSE_FIELDS[2], this@DefaultImpl.homeworld?.marshaller())
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, true, null),
        ResponseField.forObject("homeworld", "homeworld", null, true, null)
      )

      operator fun invoke(reader: ResponseReader): DefaultImpl = reader.run {
        val __typename = readString(RESPONSE_FIELDS[0])!!
        val name = readString(RESPONSE_FIELDS[1])
        val homeworld = readObject<Homeworld1>(RESPONSE_FIELDS[2]) { reader ->
          Homeworld1(reader)
        }
        DefaultImpl(
          __typename = __typename,
          name = name,
          homeworld = homeworld
        )
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<DefaultImpl> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment pilotFragment on Person {
        |  __typename
        |  name
        |  homeworld {
        |    __typename
        |    ...planetFragment
        |  }
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): PilotFragment = DefaultImpl(reader)
  }
}
