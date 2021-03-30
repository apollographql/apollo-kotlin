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
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForVariable
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterInitializer
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

internal fun dataResponseAdapterTypeSpecs(
    layout: CodegenLayout,
    dataField: IrField,
): List<TypeSpec> {
  return dataField.responseAdapterTypeSpecs(layout)
}

internal fun IrField.responseAdapterTypeSpecs(layout: CodegenLayout): List<TypeSpec> {
  return when (implementations.size){
    0 -> emptyList() // scalar
    1 -> listOf(implementations.first().adapterTypeSpec(layout))
    else -> polymorphicAdapterTypeSpecs(layout)
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

internal fun IrFieldSet.adapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.objectBuilder(modelName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(layout.fieldSetClassName(this)))
      .addProperty(responseNamesPropertySpec())
      .addFunction(readFromResponseFunSpec(layout))
      .addFunction(writeToResponseFunSpec(layout))
      .addTypes(fields.flatMap {
        it.responseAdapterTypeSpecs(layout)
      })
      .build()
}

internal fun IrFieldSet.readFromResponseCodeBlock(layout: CodegenLayout, variableInitializer: (String) -> String): CodeBlock {
  val prefix = fields.map { field ->
    CodeBlock.of(
        "var·%L:·%T·=·%L",
        kotlinNameForVariable(field.responseName),
        layout.fieldTypeName(field).copy(nullable = true),
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
                layout.variableName(field.responseName),
                field.adapterInitializer(layout)
            )
          }.joinToCode(separator = "\n", suffix = "\n")
      )
      .addStatement("else -> break")
      .endControlFlow()
      .endControlFlow()
      .build()

  val suffix = CodeBlock.builder()
      .addStatement("return·%T(", layout.fieldSetClassName(this))
      .indent()
      .add(fields.map { field ->
        CodeBlock.of(
            "%L·=·%L%L",
            layout.propertyName(field.responseName),
            layout.variableName(field.responseName),
            if (layout.fieldTypeName(field).isNullable) "" else "!!"
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

private fun IrFieldSet.readFromResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(layout.fieldSetClassName(this))
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(readFromResponseCodeBlock(layout) { "null" })
      .build()
}

private fun IrFieldSet.writeToResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, layout.fieldSetClassName(this))
      .addCode(writeToResponseCodeBlock(layout))
      .build()
}


internal fun IrFieldSet.writeToResponseCodeBlock(layout: CodegenLayout): CodeBlock {
  val builder = CodeBlock.builder()
  fields.forEach {
    builder.add(it.writeToResponseCodeBlock(layout))
  }
  return builder.build()
}

private fun IrField.writeToResponseCodeBlock(layout: CodegenLayout): CodeBlock {
  return CodeBlock.builder().apply {
    addStatement("$writer.name(%S)", responseName)
    addStatement(
        "%L.$toResponse($writer, $responseAdapterCache, $value.${layout.propertyName(responseName)})",
        adapterInitializer(layout)
    )
  }.build()
}

