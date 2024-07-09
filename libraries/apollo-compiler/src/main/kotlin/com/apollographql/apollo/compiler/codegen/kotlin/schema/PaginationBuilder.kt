package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.paginationPackageName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class PaginationBuilder(
    private val context: KotlinSchemaContext,
    private val connectionTypes: List<String>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.paginationPackageName()
  private val simpleName = layout.paginationName()

  override fun prepare() {
    context.resolver.register(ResolverKeyKind.Pagination, "", ClassName(packageName, simpleName))
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
