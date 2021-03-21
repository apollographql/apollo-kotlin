package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForProperty
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.ModelPath
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
    val modelPath: ModelPath?,
    val optional: Boolean = false,
)

fun NamedType.typeName(): TypeName {
  return if (optional) {
    Input::class.asClassName().parameterizedBy(type.typeName(null))
  } else {
    type.typeName(modelPath?.typeName())
  }
}

fun NamedType.adapterInitializer(): CodeBlock {
  return if (optional) {
    val inputFun = MemberName("com.apollographql.apollo3.api", "input")
    CodeBlock.of("%L.%M()", type.adapterInitializer(modelPath?.typeName()), inputFun)
  } else {
    type.adapterInitializer(modelPath?.typeName())
  }
}

internal fun NamedType.toParameterSpec(): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = kotlinNameForProperty(graphQlName),
          type = typeName()
      )
      .applyIf(description?.isNotBlank() == true) { addKdoc("%L\n", description!!) }
      .applyIf(optional) { defaultValue("%T", Input.Absent::class.asClassName()) }
      .build()
}
