// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.introspection_default_value.type

import com.apollographql.apollo.api.InputType
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter")
data class SampleInput(
  val user: String = "me"
) : InputType {
  override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller.invoke { writer ->
    writer.writeString("user", this@SampleInput.user)
  }
}
