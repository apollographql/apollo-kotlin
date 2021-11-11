package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrModel
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

class MonomorphicFieldResponseAdapterBuilder(
    val context: KotlinContext,
    val model: IrModel,
    val path: List<String>,
    val public: Boolean,
) : ResponseAdapterBuilder {

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
            KotlinClassNames.Adapter.parameterizedBy(
                context.resolver.resolveModel(model.id)
            )
        )
        .applyIf(!public) {
          addModifiers(KModifier.PRIVATE)
        }
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
        .addModifiers(KModifier.OVERRIDE)
        .addCode(readFromResponseCodeBlock(model, context, false))
        .build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.toJson)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(Identifier.writer, KotlinClassNames.JsonWriter)
        .addParameter(Identifier.customScalarAdapters, KotlinClassNames.CustomScalarAdapters)
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context))
        .build()
  }
}