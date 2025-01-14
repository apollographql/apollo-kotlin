package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.typeScalarPackageName
import com.apollographql.apollo.compiler.ir.IrScalar
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.ANY
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.BOOLEAN
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.DOUBLE
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.FLOAT
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.INT
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.LONG
import com.apollographql.apollo.compiler.ir.IrScalarInlineClassCoerceAs.STRING
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

internal class ScalarInlineClassBuilder(
    context: KotlinSchemaContext,
    private val scalar: IrScalar,
) : CgFileBuilder {
  private val layout = context.layout
  private val simpleName = layout.schemaTypeName(scalar.name)

  override fun prepare() {}

  override fun build(): CgFile {
    return CgFile(
        packageName = layout.typeScalarPackageName(),
        fileName = simpleName,
        typeSpecs = listOf(scalar.typeSpec())
    )
  }

  private fun IrScalar.typeSpec(): TypeSpec {
    val valueType = when (scalar.inlineClassCoerceAs) {
      STRING -> String::class
      BOOLEAN -> Boolean::class
      INT -> Int::class
      LONG -> Long::class
      FLOAT -> Float::class
      DOUBLE -> Double::class
      ANY -> Any::class
      null -> error("inlineClassCoerceAs is null")
    }.asClassName()
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(KModifier.VALUE)
        .addAnnotation(KotlinSymbols.JvmInline)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .primaryConstructor(
            FunSpec.constructorBuilder().addParameter(
                ParameterSpec(
                    name = Identifier.value,
                    type = valueType,
                )
            ).build()
        )
        .addProperty(PropertySpec.builder(Identifier.value, valueType).initializer(CodeBlock.of(Identifier.value)).build())

        .build()
  }
}
