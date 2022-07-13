package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class PaginationBuilder(
    context: KotlinContext,
    private val connectionTypes: Set<String>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.paginationPackageName()
  private val simpleName = layout.paginationName()

  override fun prepare() {
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(simpleName)
        .addProperty(connectionTypesPropertySpec())
        .build()
  }

  private fun connectionTypesPropertySpec(): PropertySpec {
    val builder = CodeBlock.builder()
    builder.add("setOf(\n")
    builder.indent()
    builder.add(
        connectionTypes.map {
          CodeBlock.of("%S", it)
        }.joinToString(", ")
    )
    builder.unindent()
    builder.add(")\n")

    return PropertySpec.builder("connectionTypes", KotlinSymbols.Set.parameterizedBy(KotlinSymbols.String))
        .initializer(builder.build())
        .build()
  }
}
