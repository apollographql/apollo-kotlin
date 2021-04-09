package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrInputField
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.IrVariable
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

class NamedType(
    val graphQlName: String,
    val description: String?,
    val deprecationReason: String?,
    val type: IrType,
    val optional: Boolean = false,
)

fun NamedType.typeName(layout: CodegenLayout): TypeName {
  return if (optional) {
    Input::class.asClassName().parameterizedBy(layout.typeTypename(type))
  } else {
    layout.typeTypename(type)
  }
}

fun NamedType.adapterInitializer(layout: CodegenLayout): CodeBlock {
  return if (optional) {
    val inputFun = MemberName("com.apollographql.apollo3.api", "input")
    CodeBlock.of("%L.%M()", type.adapterInitializer(layout, null), inputFun)
  } else {
    type.adapterInitializer(layout, null)
  }
}

internal fun NamedType.toParameterSpec(layout: CodegenLayout): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = layout.propertyName(graphQlName),
          type = typeName(layout)
      )
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(optional) { defaultValue("%T", Input.Absent::class.asClassName()) }
      .build()
}


fun IrInputField.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    optional = optional,
    description = description,
    deprecationReason = deprecationReason,
)

fun IrVariable.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    optional = optional,
    description = null,
    deprecationReason = null,
)
