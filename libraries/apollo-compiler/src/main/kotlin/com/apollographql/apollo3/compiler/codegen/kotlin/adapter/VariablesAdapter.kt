/*
 * Generates ResponseAdapters for variables/input
 */
package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.scalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeDataContext
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.requiresOptInAnnotation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.suppressDeprecationAnnotationSpec
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun List<NamedType>.variablesAdapterTypeSpec(
    context: KotlinContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
      .addSuperinterface(KotlinSymbols.VariablesAdapter.parameterizedBy(adaptedTypeName))
      .addFunction(serializeVariablesFunSpec(context, adaptedTypeName))
      .apply {
        if (this@variablesAdapterTypeSpec.any { it.deprecationReason != null }) {
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

private fun List<NamedType>.serializeVariablesFunSpec(
    context: KotlinContext,
    adaptedTypeName: TypeName,
): FunSpec {
  return FunSpec.builder(serializeVariables)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(value, adaptedTypeName)
      .addParameter(Identifier.context, KotlinSymbols.SerializeVariablesContext)
      .addCode(writeToResponseCodeBlock(context))
      .build()
}

private fun List<NamedType>.writeToResponseCodeBlock(context: KotlinContext): CodeBlock {
  val builder = CodeBlock.builder()
  builder.addStatement("val $serializeDataContext = %T(${Identifier.context}.$scalarAdapters)", KotlinSymbols.DataSerializeContext)
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
  builder.addStatement(
      "%L.$toJson($writer, $value.%N, $serializeDataContext)",
      adapterInitializer,
      propertyName,
  )
  if (type.isOptional()) {
    builder.endControlFlow()
    if (defaultValue is IrBooleanValue) {
      builder.beginControlFlow("else if (${Identifier.context}.withDefaultBooleanValues)")
      builder.addStatement("$writer.name(%S)", graphQlName)
      builder.addStatement(
          "%M.$toJson($writer, %L, $serializeDataContext)",
          KotlinSymbols.BooleanApolloAdapter,
          defaultValue.value,
      )

      builder.endControlFlow()
    }
  }

  return builder.build()
}
