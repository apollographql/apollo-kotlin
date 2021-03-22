package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.apollographql.apollo3.compiler.unified.IrField
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

fun serializeVariablesFunSpec(
    packageName: String,
    name: String,
): FunSpec {
  val adapterTypeName = ClassName(adapterPackageName(packageName), kotlinNameForVariablesAdapter(name))

  return FunSpec.builder(Identifier.serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addCode(
          "%L.toResponse(writer, ${Identifier.responseAdapterCache}, this)",
          CodeBlock.of("%T", adapterTypeName).obj(false)
      )
      .build()
}

fun adapterFunSpec(
    packageName: String,
    name: String,
    dataField: IrField,
): FunSpec {
  val adapterTypeName = ClassName(adapterPackageName(packageName), kotlinNameForResponseAdapter(name))

  return FunSpec.builder("adapter")
      .addModifiers(KModifier.OVERRIDE)
      .returns(ResponseAdapter::class.asClassName().parameterizedBy(dataField.typeName()))
      .addCode(CodeBlock.of("return·%T", adapterTypeName).obj(false))
      .build()
}

fun responseFieldsFunSpec(): FunSpec {
  return FunSpec.builder(
      "responseFields",
  )
      .addModifiers(KModifier.OVERRIDE)
      .returns(
          List::class.asClassName().parameterizedBy(
              ResponseField.FieldSet::class.asClassName(),
          )
      )
      .addCode("return %L", "emptyList()")
      .build()


}