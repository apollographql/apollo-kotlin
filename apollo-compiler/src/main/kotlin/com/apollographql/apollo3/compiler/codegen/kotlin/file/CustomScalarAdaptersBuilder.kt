package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec

internal class CustomScalarAdaptersBuilder(
    private val context: KotlinContext,
    private val scalarMapping: Map<String, ScalarInfo>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = "__CustomScalarAdapters"

  override fun prepare() {
    context.resolver.registerCustomScalarAdapters(ClassName(packageName, simpleName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        propertySpecs = listOf(propertySpec())
    )
  }

  private fun propertySpec(): PropertySpec {
    return PropertySpec.builder(simpleName, KotlinSymbols.CustomScalarAdapters)
        .initializer(
            CodeBlock.builder()
                .add("%T()\n", KotlinSymbols.CustomScalarAdaptersBuilder)
                .indent()
                .apply {
                  scalarMapping.entries.forEach {
                    val adapterInitializer = it.value.adapterInitializer
                    if (adapterInitializer is ExpressionAdapterInitializer) {
                      add(".add(%T.type, %L)\n", context.resolver.resolveSchemaType(it.key), adapterInitializer.expression)
                    }
                  }
                }
                .add(".build()\n")
                .build()
        )
        .addKdoc("A [CustomScalarAdapters] instance containing all the adapters known at build time")
        .build()
  }
}
