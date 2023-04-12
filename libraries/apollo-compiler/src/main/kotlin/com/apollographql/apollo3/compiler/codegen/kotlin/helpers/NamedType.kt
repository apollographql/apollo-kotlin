package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrBooleanValue
import com.apollographql.apollo3.compiler.ir.IrInputField
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.ir.IrVariable
import com.apollographql.apollo3.compiler.ir.isOptional
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec

internal class NamedType(
    val graphQlName: String,
    val description: String?,
    val deprecationReason: String?,
    val optInFeature: String?,
    val type: IrType,
    // Relevant only for variables
    val defaultValue: IrValue?,
)

internal fun NamedType.toParameterSpec(context: KotlinContext): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.layout.propertyName(graphQlName),
          type = context.resolver.resolveIrType(type)
      )
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .maybeAddRequiresOptIn(context.resolver, optInFeature)
      .applyIf(type.isOptional()) { defaultValue("%T", KotlinSymbols.Absent) }
      .build()
}


internal fun IrInputField.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = description,
    deprecationReason = deprecationReason,
    optInFeature = optInFeature,
    defaultValue = null,
)

internal fun IrVariable.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = null,
    deprecationReason = null,
    optInFeature = null,
    defaultValue = defaultValue,
)


internal fun List<NamedType>.writeToResponseCodeBlock(context: KotlinContext, withDefaultBooleanValues: Boolean): CodeBlock {
  val builder = CodeBlock.builder()
  forEach {
    builder.add(it.writeToResponseCodeBlock(context, withDefaultBooleanValues))
  }
  return builder.build()
}

internal fun NamedType.writeToResponseCodeBlock(context: KotlinContext, withDefaultBooleanValues: Boolean): CodeBlock {
  val adapterInitializer = context.resolver.adapterInitializer(type, false)
  val builder = CodeBlock.builder()
  val propertyName = context.layout.propertyName(graphQlName)

  if (type.isOptional()) {
    builder.beginControlFlow("if (${Identifier.value}.%N is %T)", propertyName, KotlinSymbols.Present)
  }
  builder.addStatement("${Identifier.writer}.name(%S)", graphQlName)
  builder.addStatement(
      "%L.${Identifier.toJson}(${Identifier.writer}, ${Identifier.scalarAdapters}, ${Identifier.value}.%N)",
      adapterInitializer,
      propertyName,
  )
  if (type.isOptional()) {
    builder.endControlFlow()
    if (withDefaultBooleanValues && defaultValue is IrBooleanValue) {
      builder.beginControlFlow("else if (${Identifier.scalarAdapters}.adapterContext.serializeVariablesWithDefaultBooleanValues)")
      builder.addStatement("${Identifier.writer}.name(%S)", graphQlName)
      builder.addStatement(
          "%M.${Identifier.toJson}(${Identifier.writer}, ${Identifier.scalarAdapters}, %L)",
          KotlinSymbols.BooleanApolloAdapter,
          defaultValue.value,
      )

      builder.endControlFlow()
    }
  }

  return builder.build()
}
