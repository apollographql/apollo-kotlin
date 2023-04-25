/*
 * Generates ResponseAdapters for input
 */
package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.deserializeData
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeData
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.requiresOptInAnnotation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.suppressDeprecationAnnotationSpec
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun List<NamedType>.inputAdapterTypeSpec(
    context: KotlinContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(KotlinSymbols.DataAdapter.parameterizedBy(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(context, adaptedTypeName))
      .apply {
        if (this@inputAdapterTypeSpec.any { it.deprecationReason != null }) {
          addAnnotation(suppressDeprecationAnnotationSpec)
        }
        if (any { it.optInFeature != null }) {
          val requiresOptInAnnotation = context.resolver.resolveRequiresOptInAnnotation()
          if (requiresOptInAnnotation != null) {
            addAnnotation(requiresOptInAnnotation(requiresOptInAnnotation))
          }
        }
      }
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder(deserializeData)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
    .addParameter(Identifier.context, KotlinSymbols.DeserializeDataContext)
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseFunSpec(
    context: KotlinContext,
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder(serializeData)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(value, adaptedTypeName)
      .addParameter(Identifier.context, KotlinSymbols.SerializeDataContext)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}

private fun List<NamedType>.writeToResponseCodeBlock(context: KotlinContext): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCodeBlock(context))
  }
  return builder.build()
}

private fun NamedType.writeToResponseCodeBlock(context: KotlinContext): CodeBlock {
  val adapterInitializer = context.resolver.adapterInitializer(type, false)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.isOptional()) {
    builder.beginControlFlow("if ($value.%N is %T)", propertyName, KotlinSymbols.Present)
  }
  builder.addStatement("$writer.name(%S)", graphQlName)
  builder.addSerializeStatement(type, adapterInitializer, propertyName)
  if (type.isOptional()) {
    builder.endControlFlow()
  }

  return builder.build()
}
