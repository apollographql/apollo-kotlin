package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrScalar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec

internal class CustomScalarAdaptersBuilder(
    private val context: KotlinSchemaContext,
    private val scalars: List<IrScalar>,
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
                  scalars.forEach {
                    val initializer = context.resolver.resolveScalarAdapterInitializer(it.name)
                    if (initializer != null && context.resolver.isScalarUserDefined(it.name)) {
                      add(".add(%T.type, %L)\n", context.resolver.resolveSchemaType(it.name), initializer)
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