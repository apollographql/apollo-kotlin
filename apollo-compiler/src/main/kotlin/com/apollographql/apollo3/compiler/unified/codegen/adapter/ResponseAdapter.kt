package com.apollographql.apollo3.compiler.unified.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForProperty
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariable
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterInitializer
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun dataResponseAdapterTypeSpecs(
    dataField: IrField,
): List<TypeSpec> {
  return dataField.responseAdapterTypeSpecs()
}

internal fun IrField.responseAdapterTypeSpecs(): List<TypeSpec> {
  return when (implementations.size){
    0 -> emptyList() // scalar
    1 -> listOf(implementations.first().adapterTypeSpec())
    else -> polymorphicAdapterTypeSpecs()
  }
}

internal fun IrFieldSet.responseNamesPropertySpec(): PropertySpec {
  val initializer = fields.map {
    CodeBlock.of("%S", it.responseName)
  }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

  return PropertySpec.builder(RESPONSE_NAMES, List::class.parameterizedBy(String::class))
      .initializer(initializer)
      .build()
}

internal fun IrFieldSet.adapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(modelName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(typeName()))
      .addProperty(responseNamesPropertySpec())
      .addFunction(readFromResponseFunSpec())
      .addFunction(writeToResponseFunSpec())
      .addTypes(fields.flatMap {
        it.responseAdapterTypeSpecs()
      })
      .build()
}

internal fun IrFieldSet.readFromResponseCodeBlock(variableInitializer: (String) -> String): CodeBlock {
  val prefix = fields.map { field ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        kotlinNameForVariable(field.responseName),
        field.typeName().copy(nullable = true),
        variableInitializer(field.responseName)
    )
  }.joinToCode(separator = "\n", suffix = "\n")

  val loop = CodeBlock.builder()
      .beginControlFlow("while(true)")
      .beginControlFlow("when·($reader.selectName($RESPONSE_NAMES))")
      .add(
          fields.mapIndexed { fieldIndex, field ->
            CodeBlock.of(
                "%L·->·%L·=·%L.$fromResponse($reader, $responseAdapterCache)",
                fieldIndex,
                kotlinNameForVariable(field.responseName),
                field.adapterInitializer()
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", typeName())
      .indent()
      .add(fields.map { field ->
        CodeBlock.of(
            "%L·=·%L%L",
            kotlinNameForProperty(field.responseName),
            kotlinNameForVariable(field.responseName),
            if (field.typeName().isNullable) "" else "!!"
        )
      }.joinToCode(separator = ",\n", suffix = "\n"))
      .unindent()
      .addStatement(")")
      .build()

  return CodeBlock.builder()
      .add(prefix)
      .add(loop)
      .add(suffix)
      .build()
}

private fun IrFieldSet.readFromResponseFunSpec(): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(typeName())
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(readFromResponseCodeBlock { "null" })
      .build()
}

private fun IrFieldSet.writeToResponseFunSpec(): FunSpec {
  return FunSpec.builder(toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, typeName())
      .addCode(writeToResponseCodeBlock())
      .build()
}


internal fun IrFieldSet.writeToResponseCodeBlock(): CodeBlock {
  val builder = CodeBlock.builder()
  fields.forEach {
    builder.add(it.writeToResponseCodeBlock())
  }
  return builder.build()
}

private fun IrField.writeToResponseCodeBlock(): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("$writer.name(%S)", responseName)
    addStatement(
        "%L.$toResponse($writer, $responseAdapterCache, $value.${kotlinNameForProperty(responseName)})",
        adapterInitializer()
    )
  }.build()
}

