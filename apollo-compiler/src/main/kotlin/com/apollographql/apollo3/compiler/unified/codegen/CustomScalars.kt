package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForCustomScalar
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.unified.IrCustomScalar
import com.apollographql.apollo3.compiler.unified.IrCustomScalars
import com.apollographql.apollo3.compiler.unified.TypeSet
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

internal fun IrCustomScalar.typeName() = ClassName(
    packageName = packageName,
    "CustomScalars",
    kotlinNameForCustomScalar(name)
)

internal fun IrCustomScalar.kotlinTypeName() = ClassName.bestGuess(kotlinName ?: error("no kotlinName for $name"))

fun IrCustomScalars.typeSpec() = customScalars.typeSpec()

fun IrCustomScalars.qualifiedTypeSpec() = QualifiedTypeSpec(
    packageName,
    customScalars.typeSpec()
)

private fun List<IrCustomScalar>.typeSpec(): TypeSpec {
  return TypeSpec.objectBuilder("CustomScalars")
      .addKdoc("Auto generated constants for custom scalars. Use them to register your [ResponseAdapter]s")
      .addProperties(
          map {
            PropertySpec
                .builder(kotlinNameForCustomScalar(it.name), CustomScalar::class)
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
