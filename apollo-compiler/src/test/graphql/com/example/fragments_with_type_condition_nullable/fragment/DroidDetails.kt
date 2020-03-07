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
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
data class DroidDetails(
  val __typename: String = "Droid",
  /**
   * What others call this droid
   */
  val name: String,
  /**
   * This droid's primary function
   */
  val primaryFunction: String?
) : GraphqlFragment {
  override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller.invoke { writer ->
    writer.writeString(RESPONSE_FIELDS[0], this@DroidDetails.__typename)
    writer.writeString(RESPONSE_FIELDS[1], this@DroidDetails.name)
    writer.writeString(RESPONSE_FIELDS[2], this@DroidDetails.primaryFunction)
  }

  companion object {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forString("primaryFunction", "primaryFunction", null, true, null)
        )

    val FRAGMENT_DEFINITION: String = """
        |fragment DroidDetails on Droid {
        |  __typename
        |  name
        |  primaryFunction
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader): DroidDetails = reader.run {
      val __typename = readString(RESPONSE_FIELDS[0])!!
      val name = readString(RESPONSE_FIELDS[1])!!
      val primaryFunction = readString(RESPONSE_FIELDS[2])
      DroidDetails(
        __typename = __typename,
        name = name,
        primaryFunction = primaryFunction
      )
    }
  }
}
