package com.apollographql.apollo3.compiler.codegen.kotlin.adapter

import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.withBooleanDefaultValues
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.requiresOptInAnnotation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.suppressDeprecationAnnotationSpec
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun List<NamedType>.variablesAdapterTypeSpec(
    context: KotlinContext,
    adapterName: String,
    adaptedTypeName: TypeName,
): TypeSpec {
  return TypeSpec.objectBuilder(adapterName)
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
      .addParameter(writer, KotlinSymbols.JsonWriter)
      .addParameter(value, adaptedTypeName)
      .addParameter(customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
      .addParameter(withBooleanDefaultValues, KotlinSymbols.Boolean)
      .addAnnotation(AnnotationSpec.builder(KotlinSymbols.Suppress).addMember("%S", "UNUSED_PARAMETER").addMember("%S", "UNUSED_VARIABLE").build())
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
  val adapterInitializer = context.resolver.adapterInitializer(type, false, context.jsExport, customScalarAdapters)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.isOptional()) {
    builder.beginControlFlow("if ($value.%N is %T)", propertyName, KotlinSymbols.Present)
  }
  builder.addStatement("$writer.name(%S)", graphQlName)
  builder.addSerializeStatement(adapterInitializer, propertyName)
  if (type.isOptional()) {
    builder.endControlFlow()
    if (defaultValue is IrBooleanValue) {
      builder.beginControlFlow("else if ($withBooleanDefaultValues)")
      builder.addStatement("$writer.name(%S)", graphQlName)
      builder.addStatement(
          "%M.toJson($writer, $customScalarAdapters, %L)",
          KotlinSymbols.BooleanAdapter,
          defaultValue.value,
      )

      builder.endControlFlow()
    }
  }

  return builder.build()
}
