/*
 * Generates ResponseAdapters for variables/input
 */
package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.helpers.writeToResponseCodeBlock
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName


internal fun List<NamedType>.inputAdapterTypeSpec(
    context: CgContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(context, adaptedTypeName))
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, JsonReader::class)
    .addParameter(responseAdapterCache, ResponseAdapterCache::class.asTypeName())
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseFunSpec(
    context: CgContext,
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder(toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, adaptedTypeName)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}


