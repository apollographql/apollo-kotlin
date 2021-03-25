package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

fun serializeVariablesFunSpec(
    packageName: String,
    name: String,
    isEmpty: Boolean
): FunSpec {
  val adapterTypeName = ClassName(adapterPackageName(packageName), kotlinNameForVariablesAdapter(name))

  val body = if (isEmpty) {
    CodeBlock.of("// This operation doesn't have variables")
  } else {
    CodeBlock.of(
        "%L.$toResponse($writer, $responseAdapterCache, this)",
            CodeBlock.of("%T", adapterTypeName).obj(false)
    )
  }
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addCode(body)
      .build()
}

fun adapterFunSpec(
    adapterTypeName: TypeName,
    adaptedTypeName: TypeName
): FunSpec {
  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(ResponseAdapter::class.asClassName().parameterizedBy(adaptedTypeName))
      .addCode(CodeBlock.of("return·%T", adapterTypeName).obj(false))
      .build()
}

fun responseFieldsFunSpec(typeName: TypeName): FunSpec {
  return FunSpec.builder("responseFields")
      .addModifiers(KModifier.OVERRIDE)
      .returns(
          List::class.asClassName().parameterizedBy(
              ResponseField.FieldSet::class.asClassName(),
          )
      )
      .addCode("return %T.fields.first().fieldSets", typeName)
      .build()
}
