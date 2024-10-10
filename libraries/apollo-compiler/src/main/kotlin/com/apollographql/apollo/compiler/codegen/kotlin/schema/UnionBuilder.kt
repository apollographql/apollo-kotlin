package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.abstractMapTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.concreteBuilderTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.concreteMapTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeImplementBuilderFactory
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.topLevelBuildFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

internal class UnionBuilder(
    private val context: KotlinSchemaContext,
    private val union: IrUnion,
    private val generateDataBuilders: Boolean,
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
    context.resolver.registerMapType(union.name, ClassName(packageName, mapName))
    context.resolver.registerBuilderType(union.name, ClassName(packageName, builderName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(union.typeSpec())
          if (generateDataBuilders) {
            add(abstractMapTypeSpec(context.resolver, mapName, emptyList()))
            add(
                concreteBuilderTypeSpec(
                    context = context,
                    packageName = packageName,
                    builderName = otherBuilderName,
                    mapName = otherMapName,
                    properties = emptyList(),
                    typename = null
                )
            )
            add(
                concreteMapTypeSpec(
                    resolver = context.resolver,
                    mapName = otherMapName,
                    extendsFromType = union.name,
                    implements = emptyList()
                )
            )
          }
        },
        funSpecs = mutableListOf<FunSpec>().apply {
          if (generateDataBuilders) {
            add(
                topLevelBuildFunSpec(
                    "buildOther${layout.schemaTypeName(union.name)}",
                    ClassName(packageName, otherBuilderName),
                    ClassName(packageName, otherMapName),
                    requiresTypename = true
                )
            )
          }
        }
    )
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
        .maybeImplementBuilderFactory(generateDataBuilders, ClassName(packageName, otherBuilderName))
        .build()
  }
}
