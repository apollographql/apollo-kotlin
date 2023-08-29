package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.apollographql.apollo3.compiler.GeneratedMethod
import com.apollographql.apollo3.compiler.GeneratedMethod.*
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

/**
 * Makes this [TypeSpec.Builder] a data class and add a primary constructor using the given parameter spec
 * as well as the corresponding properties
 */
internal fun TypeSpec.Builder.makeClassFromParameters(
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

internal fun TypeSpec.Builder.makeClassFromProperties(
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
      withEqualsImplementation()
      withHashCodeImplementation()
    } else {
      addModifiers(KModifier.DATA)
    }
    // Data class is mutually exclusive with the other methods
    return@apply
  }
  if (generateMethods.contains(EQUALS_HASH_CODE)) {
    withHashCodeImplementation()
    withEqualsImplementation()
  }

  if (generateMethods.contains(TO_STRING)) {
    if (propertySpecs.isNotEmpty()) {
      withToStringImplementation()
    }
  }

  if (generateMethods.contains(COPY)) {
    if (propertySpecs.isNotEmpty()) {
      withCopyImplementation()
    }
  }
}

internal fun TypeSpec.Builder.withCopyImplementation(): TypeSpec.Builder {
  // Note that we need to build the type to get its name because kotlin poet keeps the name internal to the builder
  val type = build()
  val constructorParamNames = type.primaryConstructor?.parameters?.map { it.name }?.toSet() ?: return this
  val typeName = type.name ?: return this
  val className = ClassName("", typeName)
  val constructorProperties = propertySpecs
      .excludeInternalProperties()
      .filter { constructorParamNames.contains(it.name) }

  check(constructorProperties.size == constructorParamNames.size) {
    "Cannot generate copy method for class with constructor arguments that are not properties"
  }

  return addFunction(FunSpec.builder(Identifier.copy)
      .apply {
        constructorProperties.forEach {
          addParameter(ParameterSpec.builder(it.name, it.type).defaultValue("this.%L", it.name).build())
        }
      }
      .returns(className)
      .addCode(CodeBlock.builder()
          .add("return %T(", className)
          .apply {
            constructorProperties.forEach {
              add("%L,", it.name)
            }
          }
          .add(")")
          .build()
      )
      .build())
}

internal fun TypeSpec.Builder.withEqualsImplementation(): TypeSpec.Builder {
  fun equalsCode(property: PropertySpec): CodeBlock {
    return CodeBlock
        .builder()
        .addStatement("this.%L == other.%L", property.name, property.name).build()
  }

  fun methodCode(typeName: String?): CodeBlock {
    if (typeName == null) {
      return CodeBlock.builder()
          .addStatement("return·other != null·&&·other::class·==·this::class")
          .build()
    }
    val className = ClassName("", typeName)
    return CodeBlock.builder()
        .beginControlFlow("if (other == this)")
        .addStatement("return true")
        .endControlFlow()
        .beginControlFlow("if (other is %T)", className)
        .apply {
          if (propertySpecs.isEmpty()) {
            add("return true\n")
          } else {
            add("return %L", propertySpecs
                .excludeInternalProperties()
                .map { equalsCode(it) }
                .joinToCode("&& ")
            )
          }
        }
        .endControlFlow()
        .addStatement("return false")
        .build()
  }

  return addFunction(FunSpec.builder(Identifier.equals)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("other", KotlinSymbols.Any.copy(nullable = true))
      .returns(KotlinSymbols.Boolean)
      .addCode(methodCode(build().name))
      .build())
}

internal fun TypeSpec.Builder.withHashCodeImplementation(): TypeSpec.Builder = apply {
  fun hashPropertyCode(property: PropertySpec) =
      CodeBlock.builder()
          .addStatement("${Identifier.__h} *= 31")
          .addStatement("${Identifier.__h} += %L.hashCode()", property.name)
          .build()

  fun methodCode(): CodeBlock {
    if (propertySpecs.isEmpty()) {
      return CodeBlock.builder().addStatement("return·this::class.hashCode()").build()
    }
    return CodeBlock.builder()
        .beginControlFlow("if (%L == null)", MEMOIZED_HASH_CODE_VAR)
        .addStatement("var ${Identifier.__h} = %L.hashCode()", propertySpecs.getOrNull(0)?.name ?: "null")
        .add(propertySpecs
            .drop(1)
            .excludeInternalProperties()
            .map(::hashPropertyCode)
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .addStatement("%L = ${Identifier.__h}", MEMOIZED_HASH_CODE_VAR)
        .endControlFlow()
        .addStatement("return %L!!", MEMOIZED_HASH_CODE_VAR)
        .build()
  }

  check(propertySpecs.all { !it.mutable }) {
    "Cannot generate hashCode function for class with mutable properties, memoization would fail"
  }

  if (propertySpecs.isNotEmpty()) {
    addProperty(
        PropertySpec.builder(MEMOIZED_HASH_CODE_VAR, KotlinSymbols.Int.copy(nullable = true), KModifier.PRIVATE)
            .mutable()
            .initializer("null")
            .build()
    )
  }

  addFunction(FunSpec.builder(Identifier.hashCode)
      .addModifiers(KModifier.OVERRIDE)
      .returns(KotlinSymbols.Int)
      .addCode(methodCode())
      .build()
  )
}

internal fun TypeSpec.Builder.withToStringImplementation(): TypeSpec.Builder {
  // Note that we need to build the type to get its name because kotlin poet keeps the name internal to the builder
  val name = build().name ?: return this
  fun printPropertiesTemplate() =
      "$name(" + propertySpecs
            .excludeInternalProperties()
            .joinToString(",") { "${it.name}=\$${it.name}" } + ")"

  fun methodCode() =
      CodeBlock.builder()
          .addStatement("return %P", printPropertiesTemplate())
          .build()

  return addFunction(FunSpec.builder(Identifier.toString)
          .addModifiers(KModifier.OVERRIDE)
          .returns(KotlinSymbols.String)
          .addCode(methodCode())
          .build())
}


private fun Collection<PropertySpec>.excludeInternalProperties(): Collection<PropertySpec> {
  return filterNot { it.name == MEMOIZED_HASH_CODE_VAR }
}

private const val MEMOIZED_HASH_CODE_VAR: String = "__hashCode"
