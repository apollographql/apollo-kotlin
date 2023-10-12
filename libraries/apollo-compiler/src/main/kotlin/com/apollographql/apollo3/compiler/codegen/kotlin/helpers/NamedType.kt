package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrInputField
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrValue
import com.apollographql.apollo3.compiler.ir.IrVariable
import com.apollographql.apollo3.compiler.ir.isOptional
import com.apollographql.apollo3.compiler.ir.makeNonOptional
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

internal class NamedType(
    val graphQlName: String,
    val description: String?,
    val deprecationReason: String?,
    val optInFeature: String?,
    val type: IrType,
    // Relevant only for variables
    val defaultValue: IrValue?,
)

internal fun NamedType.toParameterSpec(context: KotlinContext, withDefaultArguments: Boolean): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.layout.propertyName(graphQlName),
          type = context.resolver.resolveIrType(type, context.jsExport)
      )
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .maybeAddRequiresOptIn(context.resolver, optInFeature)
      .applyIf(type.isOptional() && withDefaultArguments) { defaultValue("%T", KotlinSymbols.Absent) }
      .build()
}

internal fun NamedType.toPropertySpec(context: KotlinContext): PropertySpec {
  val initializer = CodeBlock.builder()
  val actualType: IrType
  if (type.isOptional()) {
    initializer.add("%T", KotlinSymbols.Absent)
    actualType = type
  } else {
    initializer.add("null")
    actualType = (type as? IrNonNullType)?.ofType ?: type
  }
  return PropertySpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.layout.propertyName(graphQlName),
          type = context.resolver.resolveIrType(actualType, context.jsExport)
      )
      .mutable(true)
      .addModifiers(KModifier.PRIVATE, )
      .initializer(initializer.build())
      .build()
}

internal fun NamedType.toSetterFunSpec(context: KotlinContext): FunSpec {
  val propertyName = context.layout.propertyName(graphQlName)
  val body = CodeBlock.builder()
  val parameterType: IrType
  if (type.isOptional()) {
    body.add("this.%L·=·%T(%L)\n", propertyName, KotlinSymbols.Present, propertyName)
    parameterType = type.makeNonOptional()
  } else {
    body.add("this.%L·=·%L\n", propertyName,propertyName)
    parameterType = type
  }
  body.add("return this")
  return FunSpec
      .builder(name = propertyName)
      .returns(KotlinSymbols.Builder)
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .maybeAddRequiresOptIn(context.resolver, optInFeature)
      .addParameter(ParameterSpec(propertyName, context.resolver.resolveIrType(parameterType, context.jsExport)))
      .addCode(body.build())
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
