package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.cachePackageName
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class CacheBuilder(
    private val context: KotlinSchemaContext,
    private val maxAges: Map<String, Int>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.cachePackageName()
  private val simpleName = layout.cacheName()

  override fun prepare() {
    context.resolver.register(ResolverKeyKind.Cache, "", ClassName(packageName, simpleName))
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
        .addProperty(maxAgesPropertySpec())
        .build()
  }

  private fun maxAgesPropertySpec(): PropertySpec {
    val builder = CodeBlock.builder().apply {
      add("mapOf(\n")
      indent()
      add(
          maxAges.map {(k, v) ->
            CodeBlock.of("%S to %L", k, v)
          }.joinToString(",\n", postfix = ",\n")
      )
      unindent()
      add(")")
    }

    return PropertySpec.builder("maxAges", KotlinSymbols.Map.parameterizedBy(KotlinSymbols.String, KotlinSymbols.Int))
        .initializer(builder.build())
        .build()
  }
}
