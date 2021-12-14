package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

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
fun TypeSpec.Builder.makeDataClass(parameters: List<ParameterSpec>) = apply {
  if (parameters.isNotEmpty()) {
    addModifiers(KModifier.DATA)
  }
  primaryConstructor(FunSpec.constructorBuilder()
      .apply {
        parameters.forEach {
          addParameter(it)
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
