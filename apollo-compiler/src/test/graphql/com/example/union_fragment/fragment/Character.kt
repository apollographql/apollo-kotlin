// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.union_fragment.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.example.union_fragment.type.CustomType
import kotlin.Array
import kotlin.String
import kotlin.Suppress

/**
 * A character from the Star Wars universe
 */
@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface Character : GraphqlFragment {
  val __typename: String

  /**
   * The ID of the character
   */
  val id: String

  /**
   * The name of the character
   */
  val name: String

  /**
   * A character from the Star Wars universe
   */
  data class DefaultImpl(
    override val __typename: String = "Character",
    /**
     * The ID of the character
     */
    override val id: String,
    /**
     * The name of the character
     */
    override val name: String
  ) : Character {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@DefaultImpl.__typename)
        writer.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, this@DefaultImpl.id)
        writer.writeString(RESPONSE_FIELDS[2], this@DefaultImpl.name)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
        ResponseField.forString("name", "name", null, false, null)
      )
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment Character on Character {
        |  __typename
        |  id
        |  name
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): Character {
      return Character_ResponseAdapter.fromResponse(reader)
    }
  }
}
