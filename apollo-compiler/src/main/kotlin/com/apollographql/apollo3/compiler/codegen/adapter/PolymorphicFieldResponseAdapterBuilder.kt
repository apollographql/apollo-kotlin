package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdpaters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.__typename
import com.apollographql.apollo3.compiler.codegen.Identifier.fromResponse
import com.apollographql.apollo3.compiler.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.codegen.Identifier.responseAdapterCache
import com.apollographql.apollo3.compiler.codegen.Identifier.toResponse
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.ir.IrModelGroup
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class PolymorphicFieldResponseAdapterBuilder(
    val context: CgContext,
    val modelGroup: IrModelGroup,
    val path: List<String>,
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
            Adapter::class.asTypeName().parameterizedBy(adaptedClassName)
        )
        .addProperty(responseNamesPropertySpec())
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
        .build()
  }

  private fun responseNamesPropertySpec(): PropertySpec {
    return PropertySpec.builder(Identifier.RESPONSE_NAMES, List::class.parameterizedBy(String::class))
        .initializer("listOf(%S)", "__typename")
        .build()
  }

  private fun readFromResponseFunSpec(): FunSpec {
    return FunSpec.builder(fromResponse)
        .returns(adaptedClassName)
        .addParameter(reader, JsonReader::class)
        .addParameter(responseAdapterCache, CustomScalarAdpaters::class)
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
          "-> %T.$fromResponse($reader, $responseAdapterCache, $__typename)",
          ClassName.from(path + model.modelName),
      )
    }
    builder.endControlFlow()

    return builder.build()
  }

  private fun writeToResponseFunSpec(): FunSpec {
    return FunSpec.builder(toResponse)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(writer, JsonWriter::class.asTypeName())
        .addParameter(responseAdapterCache, CustomScalarAdpaters::class)
        .addParameter(value, adaptedClassName)
        .addCode(writeToResponseCodeBlock())
        .build()
  }

  private fun writeToResponseCodeBlock(): CodeBlock {
    val builder = CodeBlock.builder()

    builder.beginControlFlow("when($value) {")
    implementations.sortedByDescending { it.typeSet.size }.forEach { model ->
      builder.addStatement(
          "is %T -> %T.$toResponse($writer, $responseAdapterCache, $value)",
          context.resolver.resolveModel(model.id),
          ClassName.from(path + model.modelName),
      )
    }
    builder.endControlFlow()

    return builder.build()
  }
}