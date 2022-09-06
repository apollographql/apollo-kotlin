package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrModel
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

/**
 * @param addTypenameArgument whether to add a `typename` argument to the fromJson method
 * If no typename is present, we can make the generated adapter implement the [Adapter] interface
 * so that extension functions like `.obj(buffered)` can be used.
 */
internal class ImplementationAdapterBuilder(
    private val context: KotlinContext,
    private val model: IrModel,
    private val path: List<String>,
    private val addTypenameArgument: Boolean,
    private val public: Boolean,
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
        model.isFallback
    )
  }

  override fun prepare() {
    nestedAdapterBuilders.map { it.prepare() }
    context.resolver.registerModelAdapter(
        model.id,
        ClassName.from(path + adapterName)
    )
  }

  private fun responseNamesPropertySpec(model: IrModel): PropertySpec? {
    val regularProperties = model.properties.filter { !it.isSynthetic }

    if (regularProperties.isEmpty()) {
      return null
    }
    val initializer = regularProperties.map {
      CodeBlock.of("%S", it.info.responseName)
    }.joinToCode(prefix = "listOf(", separator = ", ", suffix = ")")

    return PropertySpec.builder(Identifier.RESPONSE_NAMES, KotlinSymbols.List.parameterizedBy(KotlinSymbols.String))
        .initializer(initializer)
        .build()
  }

  override fun build(): List<TypeSpec> {
    return TypeSpec.objectBuilder(adapterName)
        .apply {
          val responseNames = responseNamesPropertySpec(model)
          if (responseNames != null) {
            addProperty(responseNames)
          }
        }
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .apply {
          if (!addTypenameArgument) {
            addSuperinterface(
                KotlinSymbols.Adapter.parameterizedBy(
                    context.resolver.resolveModel(model.id)
                )
            )
          }
          if (!public) {
            addModifiers(KModifier.PRIVATE)
          }
        }
        .build()
        .let { listOf(it) }

  }

  private fun readFromResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.fromJson)
        .returns(adaptedClassName)
        .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
        .addParameter(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
        .apply {
          if (addTypenameArgument) {
            addParameter(
                ParameterSpec.builder(
                    Identifier.typename,
                    KotlinSymbols.String
                ).build()
            )
          } else {
            addModifiers(KModifier.OVERRIDE)
          }
        }
        .addCode(readFromResponseCodeBlock(model, context, addTypenameArgument))
        .build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(Identifier.toJson)
        .addParameter(Identifier.writer, KotlinSymbols.JsonWriter)
        .addParameter(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
        .addParameter(Identifier.value, adaptedClassName)
        .addCode(writeToResponseCodeBlock(model, context))
        .apply {
          if (!addTypenameArgument) {
            addModifiers(KModifier.OVERRIDE)
          }
        }
        .build()
  }
}
