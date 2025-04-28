package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.dataTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class UnionBuilder(
    private val context: KotlinSchemaContext,
    private val union: IrUnion,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(union.name)
  private val builderName = "${layout.schemaTypeName(union.name)}Builder"
  private val otherBuilderName = "Other${layout.schemaTypeName(union.name)}Builder"
  private val mapName = "${layout.schemaTypeName(union.name)}Map"
  private val otherMapName = "Other${layout.schemaTypeName(union.name)}Map"

  override fun prepare() {
    context.resolver.registerSchemaType(union.name, ClassName(packageName, simpleName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(union.typeSpec())
        },
    )
  }

  private fun IrUnion.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(dataTypeSpec())
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrUnion.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .build()
  }
}
