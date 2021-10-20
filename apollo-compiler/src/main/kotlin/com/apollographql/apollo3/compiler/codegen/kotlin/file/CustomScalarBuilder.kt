package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrCustomScalar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

class CustomScalarBuilder(
    private val context: KotlinContext,
    private val customScalar: IrCustomScalar
): CgOutputFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(name = customScalar.name)

  override fun prepare() {
    context.resolver.registerSchemaType(customScalar.name, ClassName(packageName, simpleName))
    if (customScalar.kotlinName != null) {
      context.resolver.registerCustomScalar(customScalar.name, ClassName.bestGuess(customScalar.kotlinName))
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(customScalar.typeSpec())
    )
  }

  private fun IrCustomScalar.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrCustomScalar.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec())
        .build()
  }
}
