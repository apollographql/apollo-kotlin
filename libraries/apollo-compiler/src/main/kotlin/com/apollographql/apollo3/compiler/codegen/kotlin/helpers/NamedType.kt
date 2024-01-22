package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlinPropertyName
import com.apollographql.apollo3.compiler.internal.applyIf
import com.apollographql.apollo3.compiler.ir.IrInputField
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.ir.IrVariable
import com.apollographql.apollo3.compiler.ir.nullable
import com.apollographql.apollo3.compiler.ir.optional
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
)

/**
 * @param withDefaultArguments whether or not to codegen Absent for missing arguments.
 * - true for clients
 * - false for servers
 */
internal fun NamedType.toParameterSpec(context: KotlinContext, withDefaultArguments: Boolean): ParameterSpec {
  return ParameterSpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.kotlinPropertyName(graphQlName),
          type = context.resolver.resolveIrType(type, context.jsExport)
      )
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .maybeAddRequiresOptIn(context.resolver, optInFeature)
      .applyIf(type.optional && withDefaultArguments) { defaultValue("%T", KotlinSymbols.Absent) }
      .build()
}

internal fun NamedType.toPropertySpec(context: KotlinContext): PropertySpec {
  val initializer = CodeBlock.builder()
  val actualType: IrType
  if (type.optional) {
    initializer.add("%T", KotlinSymbols.Absent)
    actualType = type
  } else {
    initializer.add("null")
    actualType = type.nullable(true)
  }
  return PropertySpec
      .builder(
          // we use property for parameters as these are ultimately data classes
          name = context.kotlinPropertyName(graphQlName),
          type = context.resolver.resolveIrType(actualType, context.jsExport)
      )
      .mutable(true)
      .addModifiers(KModifier.PRIVATE, )
      .initializer(initializer.build())
      .build()
}

internal fun NamedType.toSetterFunSpec(context: KotlinContext): FunSpec {
  val propertyName = context.kotlinPropertyName(graphQlName)
  val body = CodeBlock.builder()
  val parameterType: IrType
  if (type.optional) {
    body.add("this.%L·=·%T(%L)\n", propertyName, KotlinSymbols.Present, propertyName)
    parameterType = type.optional(false)
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
)

internal fun IrVariable.toNamedType() = NamedType(
    graphQlName = name,
    type = type,
    description = null,
    deprecationReason = null,
    optInFeature = null,
)
