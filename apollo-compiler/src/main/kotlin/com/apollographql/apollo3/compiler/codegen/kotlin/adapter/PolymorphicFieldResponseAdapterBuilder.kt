package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo3.compiler.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrModelGroup
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class PolymorphicFieldResponseAdapterBuilder(
    val context: KotlinContext,
    val modelGroup: IrModelGroup,
    val path: List<String>,
    val public: Boolean,
) : ResponseAdapterBuilder {
  private val baseModel = modelGroup.models.first {
    it.id == modelGroup.baseModelId
  }
  private val adapterName = baseModel.modelName
  private val adaptedClassName by lazy {
    context.resolver.resolveModel(baseModel.id)
  }

  private val implementations = modelGroup
      .models
      .filter { !it.isInterface }

  private val implementationAdapterBuilders = implementations.map {
    ImplementationAdapterBuilder(
        context,
        it,
        path
    )
  }

  override fun prepare() {
    context.resolver.registerModelAdapter(
        modelGroup.baseModelId,
        ClassName(
            path.first(),
            path.drop(1) + adapterName
        )
    )
    implementationAdapterBuilders.forEach {
      it.prepare()
    }
  }

  override fun build(): List<TypeSpec> {
    return listOf(typeSpec()) + implementationAdapterBuilders.map { it.build() }
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(adapterName)
        .addSuperinterface(
            KotlinSymbols.Adapter.parameterizedBy(adaptedClassName)
        )
        .applyIf(!public) {
          addModifiers(KModifier.PRIVATE)
        }
        .addProperty(responseNamesPropertySpec())
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
        .build()
  }

  private fun responseNamesPropertySpec(): PropertySpec {
    return PropertySpec.builder(Identifier.RESPONSE_NAMES, KotlinSymbols.List.parameterizedBy(KotlinSymbols.String))
        .initializer("listOf(%S)", "__typename")
        .build()
  }

  private fun readFromResponseFunSpec(): FunSpec {
    return FunSpec.builder(fromJson)
        .returns(adaptedClassName)
        .addParameter(reader, KotlinSymbols.JsonReader)
        .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
        .addModifiers(KModifier.OVERRIDE)
        .addCode(readFromResponseCodeBlock())
        .build()
  }

  private fun readFromResponseCodeBlock(): CodeBlock {
    val builder = CodeBlock.builder()

    builder.beginControlFlow("$reader.selectName(${Identifier.RESPONSE_NAMES}).also {")
    builder.beginControlFlow("check(it == 0) {")
    builder.addStatement("%S", "__typename not present in first position")
    builder.endControlFlow()
    builder.endControlFlow()
    builder.addStatement("val $__typename = reader.nextString()!!")

    builder.beginControlFlow("return when($__typename) {")
    implementations.sortedByDescending { it.typeSet.size }.forEach { model ->
      if (!model.isFallback) {
        model.possibleTypes.forEach { possibleType ->
          builder.addStatement("%S,", possibleType)
        }
      } else {
        builder.addStatement("else")
      }
      builder.addStatement(
          "-> %T.$fromJson($reader, $customScalarAdapters, $__typename)",
          ClassName.from(path + model.modelName),
      )
    }
    builder.endControlFlow()

    return builder.build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(toJson)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(writer, KotlinSymbols.JsonWriter)
        .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
        .addParameter(value, adaptedClassName)
        .addCode(writeToResponseCodeBlock())
        .build()
  }

  private fun writeToResponseCodeBlock(): CodeBlock {
    val builder = CodeBlock.builder()

    builder.beginControlFlow("when($value) {")
    implementations.sortedByDescending { it.typeSet.size }.forEach { model ->
      builder.addStatement(
          "is %T -> %T.$toJson($writer, $customScalarAdapters, $value)",
          context.resolver.resolveModel(model.id),
          ClassName.from(path + model.modelName),
      )
    }
    builder.endControlFlow()

    return builder.build()
  }
}