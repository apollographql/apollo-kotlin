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
import kotlin.Array
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
data class Starship(
  val __typename: String = "Starship",
  /**
   * The name of the starship
   */
  val name: String
) : GraphqlFragment {
  override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller.invoke { writer ->
    writer.writeString(RESPONSE_FIELDS[0], this@Starship.__typename)
    writer.writeString(RESPONSE_FIELDS[1], this@Starship.name)
  }

  companion object {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null)
        )

    val FRAGMENT_DEFINITION: String = """
        |fragment Starship on Starship {
        |  __typename
        |  name
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): Starship = reader.run {
      val __typename = readString(RESPONSE_FIELDS[0])!!
      val name = readString(RESPONSE_FIELDS[1])!!
      Starship(
        __typename = __typename,
        name = name
      )
    }
  }
}
