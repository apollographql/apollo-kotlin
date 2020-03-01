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

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
data class Character(
  val __typename: String = "Character",
  /**
   * The ID of the character
   */
  val id: String,
  /**
   * The name of the character
   */
  val name: String
) : GraphqlFragment {
  override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller.invoke { writer ->
    writer.writeString(RESPONSE_FIELDS[0], this@Character.__typename)
    writer.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, this@Character.id)
    writer.writeString(RESPONSE_FIELDS[2], this@Character.name)
  }

  companion object {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
        ResponseField.forString("name", "name", null, false, null)
        )

    val FRAGMENT_DEFINITION: String = """
        |fragment Character on Character {
        |  __typename
        |  id
        |  name
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): Character = reader.run {
      val __typename = readString(RESPONSE_FIELDS[0])!!
      val id = readCustomType<String>(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField)!!
      val name = readString(RESPONSE_FIELDS[2])!!
      Character(
        __typename = __typename,
        id = id,
        name = name
      )
    }
  }
}
