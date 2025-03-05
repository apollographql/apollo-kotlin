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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class InlineClassBuilder(
    private val context: KotlinSchemaContext,
    private val scalar: IrScalar,
    private val valueType: TypeName
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typeScalarPackageName()
  private val simpleName = layout.schemaTypeName(scalar.name)

  val className = ClassName(packageName, simpleName)

  override fun prepare() {
    context.resolver.registerScalarInlineProperty(scalar.name, Identifier.inlinePropertyValue)
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(scalar.typeSpec())
    )
  }

  private fun IrScalar.typeSpec(): TypeSpec {
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
