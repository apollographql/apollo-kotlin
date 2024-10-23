package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrInputField
import com.apollographql.apollo.compiler.ir.IrType
import com.apollographql.apollo.compiler.ir.IrVariable
import com.apollographql.apollo.compiler.ir.nullable
import com.apollographql.apollo.compiler.ir.optional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

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
          name = context.layout.propertyName(graphQlName),
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
  if (type.optional) {
    body.add("this.%N = %T(%N)\n", propertyName, KotlinSymbols.Present, propertyName)
    parameterType = type.optional(false)
  } else {
    body.add("this.%N = %N\n", propertyName, propertyName)
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


internal fun List<NamedType>.builderTypeSpec(context: KotlinContext, returnedClassName: ClassName): TypeSpec {
  return TypeSpec.classBuilder(Identifier.Builder)
      .apply {
        forEach {
          addProperty(it.toPropertySpec(context))
          addFunction(it.toSetterFunSpec(context))
        }
      }
      .addFunction(toBuildFunSpec(context, returnedClassName))
      .build()
}

private fun List<NamedType>.toBuildFunSpec(context: KotlinContext, returnedClassName: ClassName): FunSpec {
  return FunSpec.builder(Identifier.build)
      .returns(returnedClassName)
      .addCode(
          CodeBlock.builder()
              .add("return %T(\n", returnedClassName)
              .indent()
              .apply {
                forEach {
                  val propertyName = context.layout.propertyName(it.graphQlName)
                  add("%N = %N", propertyName, propertyName)
                  if (!it.type.nullable && !it.type.optional) {
                    add(" ?: error(\"missing value for $propertyName\")")
                  }
                  add(",\n")
                }
              }
              .unindent()
              .add(")")
              .build()
      )
      .build()
}