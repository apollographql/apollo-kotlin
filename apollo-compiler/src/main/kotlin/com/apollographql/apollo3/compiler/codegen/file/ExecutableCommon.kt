package com.apollographql.apollo3.compiler.codegen.file

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.MergedField
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.adapter.obj
import com.apollographql.apollo3.compiler.codegen.helpers.patchKotlinNativeOptionalArrayProperties
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

fun serializeVariablesFunSpec(
    adapterClassName: TypeName?,
    emptyMessage: String
): FunSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("""
      // $emptyMessage
    """.trimIndent())
  } else {
    CodeBlock.of(
        "%L.$toResponse($writer, $responseAdapterCache, this)",
            CodeBlock.of("%T", adapterClassName)
    )
  }
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class)
      .addParameter(responseAdapterCache, CustomScalarAdpaters::class.asTypeName())
      .addCode(body)
      .build()
}

fun adapterFunSpec(
    adapterTypeName: TypeName,
    adaptedTypeName: TypeName
): FunSpec {
  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(Adapter::class.asClassName().parameterizedBy(adaptedTypeName))
      .addCode(CodeBlock.of("return·%T", adapterTypeName).obj(false))
      .build()
}

fun responseFieldsFunSpec(typeName: TypeName): FunSpec {
  return FunSpec.builder("responseFields")
      .addModifiers(KModifier.OVERRIDE)
      .returns(
          List::class.asClassName().parameterizedBy(
              MergedField.FieldSet::class.asClassName(),
          )
      )
      .addCode("return %T.fields.first().fieldSets\n", typeName)
      .build()
}

fun TypeSpec.maybeAddFilterNotNull(generateFilterNotNull: Boolean): TypeSpec {
  if (!generateFilterNotNull) {
    return this
  }
  return patchKotlinNativeOptionalArrayProperties()
}