package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal class UnionBuilder(
    private val context: KotlinContext,
    private val union: IrUnion,
    private val generateDataBuilders: Boolean
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(union.name)

  override fun prepare() {
    context.resolver.registerSchemaType(union.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(union.name, ClassName(packageName, layout.mapName(union.name)))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(union.typeSpec())
          if (generateDataBuilders) {
            add(union.mapTypeSpec())
          }
        },
    )
  }

  private fun IrUnion.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .interfaceBuilder(layout.mapName(name))
        .build()
  }


  private fun IrUnion.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrUnion.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .build()
  }
}
