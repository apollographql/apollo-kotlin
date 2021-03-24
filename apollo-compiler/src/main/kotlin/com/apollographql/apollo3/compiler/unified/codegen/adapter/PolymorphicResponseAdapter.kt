package com.apollographql.apollo3.compiler.unified.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.codegen.helpers.adapterTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.rawTypeName
import com.apollographql.apollo3.compiler.unified.codegen.helpers.typeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun IrField.polymorphicAdapterTypeSpecs(): List<TypeSpec> {
  val implementations = implementations.map {
    it.implementationAdapterTypeSpec()
  }

  return listOf(polymorphicAdapterTypeSpec()) + implementations
}

private fun IrField.polymorphicAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(baseFieldSet!!.modelName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(baseFieldSet.typeName()))
      .addProperty(responseNamesPropertySpec())
      .addFunction(polymorphicReadFromResponseFunSpec())
      .addFunction(polymorphicWriteToResponseFunSpec())
      .build()
}

private fun responseNamesPropertySpec(): PropertySpec {
  return PropertySpec.builder(RESPONSE_NAMES, List::class.parameterizedBy(String::class))
      .initializer("listOf(%S)", "__typename")
      .build()
}

private fun IrField.polymorphicReadFromResponseFunSpec(): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(baseFieldSet!!.typeName())
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(polymorphicReadFromResponseCodeBlock())
      .build()
}

private fun IrField.polymorphicReadFromResponseCodeBlock(): CodeBlock {
  val builder = CodeBlock.builder()

  builder.beginControlFlow("$reader.selectName($RESPONSE_NAMES).also {")
  builder.beginControlFlow("check(it == 0) {")
  builder.addStatement("%S", "__typename not present in first position")
  builder.endControlFlow()
  builder.endControlFlow()
  builder.addStatement("val $__typename = reader.nextString()!!")

  builder.beginControlFlow("return when($__typename) {")
  implementations.sortedByDescending { it.typeSet.size }.forEach { fieldSet ->
    if (fieldSet.typeSet.size > 1) {
      fieldSet.possibleTypes.forEach { possibleType ->
        builder.addStatement("%S,", possibleType)
      }
    } else {
      builder.addStatement("else")
    }
    builder.addStatement("-> %T.$fromResponse($reader, $responseAdapterCache, $__typename)", fieldSet.adapterTypeName())
  }
  builder.endControlFlow()

  return builder.build()
}

private fun IrField.polymorphicWriteToResponseFunSpec(): FunSpec {
  return FunSpec.builder(toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, rawTypeName())
      .addCode(polymorphicWriteToResponseCodeBlock())
      .build()
}

private fun IrField.polymorphicWriteToResponseCodeBlock(): CodeBlock {
  val builder = CodeBlock.builder()

  builder.beginControlFlow("when($value) {")
  implementations.sortedByDescending { it.typeSet.size }.forEach { fieldSet ->
    builder.addStatement("is %T -> %T.$toResponse($writer, $responseAdapterCache, $value)", fieldSet.typeName(), fieldSet.adapterTypeName())
  }
  builder.endControlFlow()

  return builder.build()
}

private fun IrFieldSet.implementationAdapterTypeSpec(): TypeSpec {
  return TypeSpec.objectBuilder(modelName)
      .addProperty(responseNamesPropertySpec())
      .addFunction(implementationReadFromResponseFunSpec())
      .addFunction(implementationWriteToResponseFunSpec())
      .addTypes(fields.flatMap {
        it.responseAdapterTypeSpecs()
      })
      .build()

}

private fun IrFieldSet.implementationReadFromResponseFunSpec(): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(typeName())
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(__typename, String::class)
      .addCode(readFromResponseCodeBlock {
        if (it == "__typename") {
          __typename
        } else {
          "null"
        }
      })
      .build()
}

private fun IrFieldSet.implementationWriteToResponseFunSpec(): FunSpec {
  return FunSpec.builder(toResponse)
      .addParameter(Identifier.writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(Identifier.value, typeName())
      .addCode(writeToResponseCodeBlock())
      .build()
}