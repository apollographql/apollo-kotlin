package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.ir.IrModel
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class MonomorphicFieldResponseAdapterBuilder(
    val context: CgContext,
    val model: IrModel,
    val path: List<String>,
) : ResponseAdapterBuilder {

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

  override fun prepare() {
    context.resolver.registerModelAdapter(
        model.id,
        ClassName.from(path + adapterName)
    )
    nestedAdapterBuilders.map { it.prepare() }
  }

  override fun build(): List<TypeSpec> {
    return listOf(typeSpec())
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(adapterName)
        .addSuperinterface(
            ResponseAdapter::class.asTypeName().parameterizedBy(
                context.resolver.resolveModel(model.id)
            )
        )
        .addProperty(responseNamesPropertySpec(model))
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .build()
  }

  private fun readFromResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.fromResponse)
        .returns(adaptedClassName)
        .addParameter(Identifier.reader, JsonReader::class)
        .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
        .addModifiers(KModifier.OVERRIDE)
        .addCode(readFromResponseCodeBlock(model, context, false))
        .build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.toResponse)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(Identifier.writer, JsonWriter::class.asTypeName())
        .addParameter(Identifier.responseAdapterCache, ResponseAdapterCache::class)
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context))
        .build()
  }
}