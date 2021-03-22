package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariablesAdapter
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

fun serializeVariablesFunSpec(
    adapterTypeName: TypeName,
): FunSpec {
  val serializerClassName = ClassName("$packageName.adapter", kotlinNameForVariablesAdapter(name))

  return FunSpec.builder(Identifier.serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
      .addCode(
          "%L.toResponse(writer, ${Identifier.responseAdapterCache}, this)",
          CodeBlock.of("%T", serializerClassName).obj(false)
      )
      .build()
}