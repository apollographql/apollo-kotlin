package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.ir.IrModel
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class ImplementationAdapterBuilder(
    val context: CgContext,
    val model: IrModel,
    val path: List<String>
) {
  private val adapterName = model.modelName
  private val adaptedClassName by lazy {
    context.resolver.resolveModel(model.id)
  }

  private val nestedAdapterBuilders = model.modelGroups.map {
    ResponseAdapterBuilder.create(
        context,
        it,
        path + adapterName
    )
  }

  fun prepare() {
    nestedAdapterBuilders.map { it.prepare() }
  }

  fun build(): TypeSpec {
    return TypeSpec.objectBuilder(adapterName)
        .addProperty(responseNamesPropertySpec(model))
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .build()

  }

  private fun readFromResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.fromJson)
        .returns(adaptedClassName)
        .addParameter(Identifier.reader, JsonReader::class)
        .addParameter(Identifier.customScalarAdapters, CustomScalarAdapters::class)
        .addParameter(Identifier.typename, String::class)
        .addCode(readFromResponseCodeBlock(model, context, true))
        .build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.toJson)
        .addParameter(Identifier.writer, JsonWriter::class.asTypeName())
        .addParameter(Identifier.customScalarAdapters, CustomScalarAdapters::class)
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context))
        .build()
  }
}