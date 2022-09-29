package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

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
fun TypeSpec.Builder.makeDataClass(
    parameters: List<ParameterSpec>,
    addJvmOverloads: Boolean = false,
) = apply {
  if (parameters.isNotEmpty()) {
    addModifiers(KModifier.DATA)
  } else {
    // Can't use a data class: manually add equals/hashCode based on the class type
    addFunction(equalsFunSpec())
    addFunction(hashCodeFunSpec())
  }
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
}

fun TypeSpec.Builder.makeDataClassFromProperties(properties: List<PropertySpec>) = apply {
  if (properties.isNotEmpty()) {
    addModifiers(KModifier.DATA)
  }
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
}

private fun equalsFunSpec() = FunSpec.builder(Identifier.equals)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter("other", KotlinSymbols.Any.copy(nullable = true))
    .returns(KotlinSymbols.Boolean)
    .addStatement("return·other != null·&&·other::class·==·this::class")
    .build()

private fun hashCodeFunSpec() = FunSpec.builder(Identifier.hashCode)
    .addModifiers(KModifier.OVERRIDE)
    .returns(KotlinSymbols.Int)
    .addStatement("return·this::class.hashCode()")
    .build()
