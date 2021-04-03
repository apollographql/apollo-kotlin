package com.apollographql.apollo3.compiler.unified.codegen.file

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.unified.codegen.CgContext
import com.apollographql.apollo3.compiler.unified.codegen.CgFile
import com.apollographql.apollo3.compiler.unified.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.unified.ir.IrCustomScalar
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class CustomScalarsBuilder(
    private val context: CgContext,
    private val customScalars: List<IrCustomScalar>,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  override fun prepare() {
    customScalars.forEach {
      context.resolver.registerCustomScalar(
          it.name,
          it.kotlinName
      )
      context.resolver.registerCustomScalarConst(
          it.name,
          MemberName(
              ClassName(
                  packageName,
                  layout.customScalarsName()
              ),
              layout.customScalarName(it.name)
          )
      )
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = layout.customScalarsName(),
        typeSpecs = listOf(customScalars.typeSpec())
    )
  }

  private fun List<IrCustomScalar>.typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(layout.customScalarsName())
        .addKdoc("Auto generated constants for custom scalars. Use them to register your [ResponseAdapter]s")
        .addProperties(
            map {
              PropertySpec
                  .builder(layout.customScalarName(it.name), CustomScalar::class)
                  .maybeAddDescription(it.description)
                  .maybeAddDeprecation(it.deprecationReason)
                  .applyIf(it.kotlinName == null) {
                    addKdoc("\n\nNo mapping was registered for this custom scalar. Use the Gradle plugin [customScalarsMapping] option to add one.")
                  }
                  .initializer("%T(%S, %S)", CustomScalar::class.asTypeName(), it.name, it.kotlinName)
                  .build()
            }
        )
        .build()
  }
}
