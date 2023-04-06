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

internal class ScalarAdaptersBuilder(
    private val context: KotlinContext,
    private val scalarMapping: Map<String, ScalarInfo>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = "__ScalarAdapters"

  override fun prepare() {
    context.resolver.registerScalarAdapters(ClassName(packageName, simpleName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        propertySpecs = listOf(propertySpec())
    )
  }

  private fun propertySpec(): PropertySpec {
    return PropertySpec.builder(simpleName, KotlinSymbols.ScalarAdapters)
        .initializer(
            CodeBlock.builder()
                .add("%T()\n", KotlinSymbols.ScalarAdaptersBuilder)
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
        .addKdoc("A [ScalarAdapters] instance containing all the adapters known at build time")
        .build()
  }
}
