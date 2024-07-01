/*
 * Generates ResponseAdapters for input
 */
package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.fromJson
import com.apollographql.apollo.compiler.codegen.Identifier.toJson
import com.apollographql.apollo.compiler.codegen.Identifier.value
import com.apollographql.apollo.compiler.codegen.Identifier.writer
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
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
      .addSuperinterface(KotlinSymbols.Adapter.parameterizedBy(adaptedTypeName))
      .addFunction(notImplementedFromResponseFunSpec(adaptedTypeName))
      .addFunction(writeToResponseFunSpec(context, adaptedTypeName))
      .apply {
        addSuppressions(
            deprecation = this@inputAdapterTypeSpec.any { it.deprecationReason != null }
        )
        if (any { it.optInFeature != null }) {
          val requiresOptInAnnotation = context.resolver.resolveRequiresOptInAnnotation()
          if (requiresOptInAnnotation != null) {
            addAnnotation(requiresOptInAnnotation(requiresOptInAnnotation))
          }
        }
      }
      .build()
}

private fun notImplementedFromResponseFunSpec(adaptedTypeName: TypeName) = FunSpec.builder(fromJson)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
    .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
    .returns(adaptedTypeName)
    .addCode("throw %T(%S)", ClassName("kotlin", "IllegalStateException"), "Input type used in output position")
    .build()


private fun List<NamedType>.writeToResponseFunSpec(
    context: KotlinContext,
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder(toJson)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
      .addParameter(value, adaptedTypeName)
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
  val adapterInitializer = context.resolver.adapterInitializer(type, false, context.jsExport)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.optional) {
    builder.beginControlFlow("if ($value.%N is %T)", propertyName, KotlinSymbols.Present)
  }
  builder.addStatement("$writer.name(%S)", graphQlName)
  builder.addSerializeStatement(adapterInitializer, propertyName)
  if (type.optional) {
    builder.endControlFlow()
  }

  return builder.build()
}
