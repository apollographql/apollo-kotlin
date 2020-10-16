// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_with_type_condition_nullable.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress

/**
 * A humanoid creature from the Star Wars universe
 */
@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface HumanDetails : GraphqlFragment {
  val __typename: String

  /**
   * What this human calls themselves
   */
  val name: String

  /**
   * Height in the preferred unit, default is meters
   */
  val height: Double?

  /**
   * A humanoid creature from the Star Wars universe
   */
  data class DefaultImpl(
    override val __typename: String = "Human",
    /**
     * What this human calls themselves
     */
    override val name: String,
    /**
     * Height in the preferred unit, default is meters
     */
    override val height: Double?
  ) : HumanDetails {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@DefaultImpl.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@DefaultImpl.name)
        writer.writeDouble(RESPONSE_FIELDS[2], this@DefaultImpl.height)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forDouble("height", "height", null, true, null)
      )
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment HumanDetails on Human {
        |  __typename
        |  name
        |  height
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): HumanDetails {
      return HumanDetails_ResponseAdapter.fromResponse(reader)
    }
  }
}
