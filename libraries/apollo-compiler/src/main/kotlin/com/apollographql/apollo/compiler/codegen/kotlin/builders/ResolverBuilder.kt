package com.apollographql.apollo.compiler.codegen.kotlin.builders

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinDataBuilderContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.builderResolverPackageName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.withIndent

internal class ResolverBuilder(
    private val context: KotlinDataBuilderContext,
    private val possibleTypes: Map<String, List<String>>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.builderResolverPackageName()
  private val simpleName = "DefaultFakeResolver"

  override fun prepare() {}
  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        propertySpecs = listOf(propertySpec()),
        typeSpecs = listOf(typeSpec()),
    )
  }
  private fun propertySpec(): PropertySpec {
    return PropertySpec.builder("possibleTypes", KotlinSymbols.Map.parameterizedBy(KotlinSymbols.String, KotlinSymbols.List.parameterizedBy(KotlinSymbols.String)))
        .initializer(
            buildCodeBlock {
              add("mapOf(\n")
              withIndent {
                possibleTypes.entries.forEach {
                  add("%S to listOf(%L),\n", it.key, it.value.joinToCode(",") { CodeBlock.of("%S", it) })
                }
              }
              add(")\n")
            }
        )
        .build()
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .superclass(KotlinSymbols.BaseFakeResolver)
        .addModifiers(KModifier.OPEN)
        .addSuperclassConstructorParameter("possibleTypes")
        .build()
  }
}