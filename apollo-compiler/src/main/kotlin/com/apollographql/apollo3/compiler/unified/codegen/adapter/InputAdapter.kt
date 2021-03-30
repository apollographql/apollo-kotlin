/*
 * Generates ResponseAdapters for variables/input
 */
package com.apollographql.apollo3.compiler.unified.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.unified.ClassLayout
import com.apollographql.apollo3.compiler.unified.codegen.helpers.NamedType
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterInitializer
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName


internal fun List<NamedType>.inputAdapterTypeSpec(
    layout: ClassLayout,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(layout, adaptedTypeName))
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, JsonReader::class)
    .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class.asTypeName())
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseFunSpec(
    layout: ClassLayout,
    adaptedTypeName: TypeName
): FunSpec {
  return FunSpec.builder(Identifier.toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(Identifier.writer, JsonWriter::class.asTypeName())
      .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(Identifier.value, adaptedTypeName)
      .addCode(writeToResponseCodeBlock(layout))
      .build()
}


private fun List<NamedType>.writeToResponseCodeBlock(layout: ClassLayout): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCodeBlock(layout))
  }
  return builder.build()
}

private fun NamedType.writeToResponseCodeBlock(layout: ClassLayout): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("${Identifier.writer}.name(%S)", graphQlName)
    addStatement(
        "%L.${Identifier.toResponse}(${Identifier.writer}, ${Identifier.responseAdapterCache}, value.${layout.propertyName(graphQlName)})",
        adapterInitializer(layout)
    )
  }.build()
}

