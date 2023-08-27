package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.GeneratedMethod.*
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Makes this [TypeSpec.Builder] a data class and add a primary constructor using the given parameter spec
 * as well as the corresponding properties
 */
fun TypeSpec.Builder.makeClassFromParameters(
    generateMethods: List<GeneratedMethod>,
    parameters: List<ParameterSpec>,
    addJvmOverloads: Boolean = false,
) = apply {
  primaryConstructor(FunSpec.constructorBuilder()
      .apply {
        var hasDefaultValues = false
        parameters.forEach {
          addParameter(it)
          hasDefaultValues = hasDefaultValues || it.defaultValue != null
        }

        if (addJvmOverloads && hasDefaultValues) {
          addAnnotation(KotlinSymbols.JvmOverloads)
        }
      }
      .build())
  parameters.forEach {
    addProperty(PropertySpec.builder(it.name, it.type)
        .initializer(CodeBlock.of(it.name))
        .build())
  }
  addGeneratedMethods(generateMethods)
}

fun TypeSpec.Builder.makeClassFromProperties(
    generateMethods: List<GeneratedMethod>,
    properties: List<PropertySpec>,
) = apply {
  primaryConstructor(FunSpec.constructorBuilder()
      .apply {
        properties.forEach {
          addParameter(it.name, it.type)
        }
      }
      .build())

  properties.forEach {
    addProperty(it.toBuilder(it.name)
        .initializer(CodeBlock.of(it.name))
        .build()
    )
  }
  addGeneratedMethods(generateMethods)
}

fun TypeSpec.Builder.addGeneratedMethods(generateMethods: List<GeneratedMethod>) = apply {
  if (generateMethods.contains(DATA_CLASS)) {
    if (propertySpecs.isEmpty()) {
      addFunction(emptyPropertiesEqualsFunSpec())
      addFunction(emptyPropertiesHashCodeFunSpec())
    } else {
      addModifiers(KModifier.DATA)
    }
    // Data class is mutually exclusive with the other modifiers
    return@apply
  }
  if (generateMethods.contains(EQUALS_HASH_CODE)) {
    if (propertySpecs.isEmpty()) {
      addFunction(emptyPropertiesEqualsFunSpec())
      addFunction(emptyPropertiesHashCodeFunSpec())
    } else {
      withHashCodeImplementation()
      // TODO
    }
  }

  if (generateMethods.contains(TO_STRING)) {
    if (propertySpecs.isNotEmpty()) {
      // TODO
    }
  }

  if (generateMethods.contains(COPY)) {
    if (propertySpecs.isNotEmpty()) {
      // TODO
    }
  }
}

private fun emptyPropertiesEqualsFunSpec() = FunSpec.builder(Identifier.equals)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter("other", KotlinSymbols.Any.copy(nullable = true))
    .returns(KotlinSymbols.Boolean)
    .addStatement("return·other != null·&&·other::class·==·this::class")
    .build()

private fun emptyPropertiesHashCodeFunSpec() = FunSpec.builder(Identifier.hashCode)
    .addModifiers(KModifier.OVERRIDE)
    .returns(KotlinSymbols.Int)
    .addStatement("return·this::class.hashCode()")
    .build()

fun TypeSpec.Builder.withHashCodeImplementation(): TypeSpec.Builder {
  fun hashPropertyCode(property: PropertySpec) =
      CodeBlock.builder()
          .addStatement("${Identifier.__h} *= 1000003")
          .let {
            if (property.type.isNullable) {
              it.addStatement("${Identifier.__h} = ${Identifier.__h}.xor(%L?.hashCode() ?: 0)", property.name)
            } else {
              it.addStatement("${Identifier.__h} = ${Identifier.__h}.xor(%L.hashCode())", property.name)
            }
          }.build()

  fun methodCode() =
      CodeBlock.builder()
          .beginControlFlow("if (%L == null)", MEMOIZED_HASH_CODE_VAR)
          .addStatement("var ${Identifier.__h}: Int = 1")
          .add(propertySpecs
              .filter { it.name != MEMOIZED_HASH_CODE_VAR }
              .map(::hashPropertyCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .addStatement("%L = ${Identifier.__h}", MEMOIZED_HASH_CODE_VAR)
          .endControlFlow()
          .addStatement("return %L!!", MEMOIZED_HASH_CODE_VAR)
          .build()

  return addProperty(
          PropertySpec.builder(MEMOIZED_HASH_CODE_VAR, KotlinSymbols.Int.copy(nullable = true), KModifier.PRIVATE)
              .mutable()
              .initializer("null")
              .build()
      )
      .addFunction(FunSpec.builder("hashCode")
          .addModifiers(KModifier.OVERRIDE)
          .returns(KotlinSymbols.Int)
          .addCode(methodCode())
          .build()
      )
}

private const val MEMOIZED_HASH_CODE_VAR: String = "__hashCode"
private const val MEMOIZED_TO_STRING_VAR: String = "__toString"
