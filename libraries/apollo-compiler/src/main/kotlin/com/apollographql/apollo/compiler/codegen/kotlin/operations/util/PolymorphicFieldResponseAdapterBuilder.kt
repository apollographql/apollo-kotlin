package com.apollographql.apollo.compiler.codegen.kotlin.operations.util

import com.apollographql.apollo.compiler.codegen.Identifier.__typename
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.reader
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.from
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.typenameFromReaderCodeBlock
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrModelGroup
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec

internal class PolymorphicFieldResponseAdapterBuilder(
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
        context = context,
        model = it,
        path = path,
        addTypenameArgument = true,
        public = true, // Should we switch to private here?
    )
  }

  private val nestedAdapterBuilders: List<ResponseAdapterBuilder> = modelGroup
      .models
      .filter { it.isInterface }
      .flatMap { it.modelGroups }
      .mapNotNull {
        /**
         * For experimental_operationBasedWithInterfaces codegen, the interface might contain classes that are reused by other
         * models, so we need to build the adapters
         */
        if (it.models.any { !it.isInterface }) {
          ResponseAdapterBuilder.create(
              context,
              it,
              path + adapterName,
              true
          )
        } else {
          null
        }
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
    nestedAdapterBuilders.forEach {
      it.prepare()
    }
  }

  override fun build(): List<TypeSpec> {
    return listOf(typeSpec()) + implementationAdapterBuilders.flatMap { it.build() }
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(adapterName)
        .addSuperinterface(
            KotlinSymbols.Adapter.parameterizedBy(adaptedClassName)
        )
        .applyIf(!public) {
          addModifiers(KModifier.PRIVATE)
        }
        .addTypes(nestedAdapterBuilders.flatMap { it.build() })
        .addFunction(readFromResponseFunSpec())
        .addFunction(writeToResponseFunSpec())
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

    builder.add(typenameFromReaderCodeBlock())

    builder.beginControlFlow("return when($__typename) {")
    implementations.sortedByDescending {
      it.typeSet.size
    }.sortedByDescending {
      // make sure the fallback type is always last so that the else is the last branch
     if (it.isFallback) 0 else 1
    }.forEach { model ->
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
