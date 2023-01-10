package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrScalar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class ScalarBuilder(
    private val context: KotlinContext,
    private val customScalar: IrScalar,
    private val targetTypeName: String?,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = prefixBuiltinScalarNames(layout.compiledTypeName(customScalar.name))

  private fun prefixBuiltinScalarNames(name: String): String {
    // Kotlin Multiplatform won't build with class names that clash with Kotlin types (String, Int, etc.).
    // For consistency, do this for all built-in scalars, including ID.
    if (name in arrayOf("String", "Boolean", "Int", "Float", "ID")) {
      return "GraphQL$name"
    }
    return name
  }

  override fun prepare() {
    context.resolver.registerSchemaType(customScalar.name, ClassName(packageName, simpleName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(customScalar.typeSpec())
    )
  }

  private fun IrScalar.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrScalar.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(targetTypeName))
        .build()
  }
}
