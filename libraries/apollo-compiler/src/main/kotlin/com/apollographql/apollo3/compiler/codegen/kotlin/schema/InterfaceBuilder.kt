package com.apollographql.apollo3.compiler.codegen.kotlin.schema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.abstractMapTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.concreteBuilderTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.concreteMapTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeImplementBuilderFactory
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.topLevelBuildFunSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo3.compiler.codegen.typePackageName
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

internal class InterfaceBuilder(
    private val context: KotlinSchemaContext,
    private val iface: IrInterface,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(iface.name)
  private val builderName = "${iface.name.capitalizeFirstLetter()}Builder"
  private val otherBuilderName = "Other${iface.name.capitalizeFirstLetter()}Builder"
  private val mapName = "${iface.name.capitalizeFirstLetter()}Map"
  private val otherMapName = "Other${iface.name.capitalizeFirstLetter()}Map"

  override fun prepare() {
    context.resolver.registerSchemaType(iface.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(iface.name, ClassName(packageName, mapName))
    context.resolver.registerBuilderType(iface.name, ClassName(packageName, builderName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(iface.typeSpec())
          if (generateDataBuilders) {
            add(abstractMapTypeSpec(context.resolver, mapName, iface.implements))
            add(
                concreteBuilderTypeSpec(
                    context = context,
                    packageName = packageName,
                    builderName = otherBuilderName,
                    mapName = otherMapName,
                    properties = iface.mapProperties,
                    typename = null
                )
            )
            add(
                concreteMapTypeSpec(
                    resolver = context.resolver,
                    mapName = otherMapName,
                    extendsFromType = iface.name,
                    implements = iface.implements
                )
            )
          }
        },
        funSpecs = mutableListOf<FunSpec>().apply {
          if (generateDataBuilders) {
            add(
                topLevelBuildFunSpec(
                    "buildOther${iface.name.capitalizeFirstLetter()}",
                    ClassName(packageName, otherBuilderName),
                    ClassName(packageName, otherMapName),
                    requiresTypename = true
                )
            )
          }
        }
    )
  }

  private fun IrInterface.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrInterface.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .maybeImplementBuilderFactory(generateDataBuilders, ClassName(packageName, otherBuilderName))
        .build()
  }
}
