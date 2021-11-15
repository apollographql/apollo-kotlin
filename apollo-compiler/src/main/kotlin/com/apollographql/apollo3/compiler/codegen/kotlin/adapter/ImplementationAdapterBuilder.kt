package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrModel
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * For responseBased codegen, generates an adapter for an implementation
 */
class ImplementationAdapterBuilder(
    val context: KotlinContext,
    val model: IrModel,
    val path: List<String>,
) {
  private val adapterName = model.modelName
  private val adaptedClassName by lazy {
    context.resolver.resolveModel(model.id)
  }

  private val nestedAdapterBuilders = model.modelGroups.map {
    ResponseAdapterBuilder.create(
        context,
        it,
        path + adapterName,
        false
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
        .addParameter(Identifier.reader, KotlinClassNames.JsonReader)
        .addParameter(Identifier.customScalarAdapters, KotlinClassNames.CustomScalarAdapters)
        .addParameter(Identifier.typename, KotlinClassNames.String)
        .addCode(readFromResponseCodeBlock(model, context, true))
        .build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.toJson)
        .addParameter(Identifier.writer, KotlinClassNames.JsonWriter)
        .addParameter(Identifier.customScalarAdapters, KotlinClassNames.CustomScalarAdapters)
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context))
        .build()
  }
}