package com.apollographql.apollo3.compiler.unified.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.RESPONSE_NAMES
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.value
import com.apollographql.apollo3.compiler.backend.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.unified.CodegenLayout
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun IrField.polymorphicAdapterTypeSpecs(layout: CodegenLayout): List<TypeSpec> {
  val implementations = implementations.map {
    it.implementationAdapterTypeSpec(layout)
  }

  return listOf(polymorphicAdapterTypeSpec(layout)) + implementations
}

private fun IrField.polymorphicAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.objectBuilder(typeFieldSet!!.modelName)
      .addSuperinterface(ResponseAdapter::class.asTypeName().parameterizedBy(layout.fieldSetClassName(typeFieldSet)))
      .addProperty(responseNamesPropertySpec())
      .addFunction(polymorphicReadFromResponseFunSpec(layout))
      .addFunction(polymorphicWriteToResponseFunSpec(layout))
      .build()
}

private fun responseNamesPropertySpec(): PropertySpec {
  return PropertySpec.builder(RESPONSE_NAMES, List::class.parameterizedBy(String::class))
      .initializer("listOf(%S)", "__typename")
      .build()
}

private fun IrField.polymorphicReadFromResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(layout.fieldSetClassName(typeFieldSet!!))
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(polymorphicReadFromResponseCodeBlock(layout))
      .build()
}

private fun IrField.polymorphicReadFromResponseCodeBlock(layout: CodegenLayout): CodeBlock {
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
    builder.addStatement("-> %T.$fromResponse($reader, $responseAdapterCache, $__typename)", layout.fieldSetAdapterClassName(fieldSet))
  }
  builder.endControlFlow()

  return builder.build()
}

private fun IrField.polymorphicWriteToResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(toResponse)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, layout.fieldSetClassName(typeFieldSet!!))
      .addCode(polymorphicWriteToResponseCodeBlock(layout))
      .build()
}

private fun IrField.polymorphicWriteToResponseCodeBlock(layout: CodegenLayout): CodeBlock {
  val builder = CodeBlock.builder()

  builder.beginControlFlow("when($value) {")
  implementations.sortedByDescending { it.typeSet.size }.forEach { fieldSet ->
    builder.addStatement("is %T -> %T.$toResponse($writer, $responseAdapterCache, $value)", layout.fieldSetClassName(fieldSet), layout.fieldSetAdapterClassName(fieldSet))
  }
  builder.endControlFlow()

  return builder.build()
}

private fun IrFieldSet.implementationAdapterTypeSpec(layout: CodegenLayout): TypeSpec {
  return TypeSpec.objectBuilder(modelName)
      .addProperty(responseNamesPropertySpec())
      .addFunction(implementationReadFromResponseFunSpec(layout))
      .addFunction(implementationWriteToResponseFunSpec(layout))
      .addTypes(fields.flatMap {
        it.responseAdapterTypeSpecs(layout)
      })
      .build()

}

private fun IrFieldSet.implementationReadFromResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(fromResponse)
      .returns(layout.fieldSetClassName(this))
      .addParameter(reader, JsonReader::class)
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(__typename, String::class)
      .addCode(readFromResponseCodeBlock(layout) {
        if (it == "__typename") {
          __typename
        } else {
          "null"
        }
      })
      .build()
}

private fun IrFieldSet.implementationWriteToResponseFunSpec(layout: CodegenLayout): FunSpec {
  return FunSpec.builder(toResponse)
      .addParameter(writer, JsonWriter::class.asTypeName())
      .addParameter(responseAdapterCache, ResponseAdapterCache::class)
      .addParameter(value, layout.fieldSetClassName(this))
      .addCode(writeToResponseCodeBlock(layout))
      .build()
}