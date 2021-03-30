package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.unified.ClassLayout
import com.apollographql.apollo3.compiler.unified.IrCustomScalar
import com.apollographql.apollo3.compiler.unified.IrCustomScalars
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.unified.codegen.helpers.maybeAddDescription
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName


internal fun IrCustomScalar.kotlinTypeName() = ClassName.bestGuess(kotlinName ?: error("no kotlinName for $name"))

fun IrCustomScalars.typeSpec(classLayout: ClassLayout) = customScalars.typeSpec(classLayout)

fun IrCustomScalars.qualifiedTypeSpec(classLayout: ClassLayout) = ApolloFileSpec(
    classLayout.customScalarsClassName().packageName,
    customScalars.typeSpec(classLayout)
)

private fun List<IrCustomScalar>.typeSpec(layout: ClassLayout): TypeSpec {
  return TypeSpec.objectBuilder(layout.customScalarsClassName())
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
