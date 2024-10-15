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
import com.apollographql.apollo.compiler.ir.IrInterface
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
  private val builderName = "${layout.schemaTypeName(iface.name)}Builder"
  private val otherBuilderName = "Other${layout.schemaTypeName(iface.name)}Builder"
  private val mapName = "${layout.schemaTypeName(iface.name)}Map"
  private val otherMapName = "Other${layout.schemaTypeName(iface.name)}Map"

  override fun prepare() {
    context.resolver.registerSchemaType(iface.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(iface.name, ClassName(packageName, mapName))
    context.resolver.registerBuilderType(iface.name, ClassName(packageName, builderName))
    for (fieldDefinition in iface.fieldDefinitions) {
      fieldDefinition.argumentDefinitions.forEach { argumentDefinition ->
        context.resolver.registerArgumentDefinition(argumentDefinition.id, ClassName(packageName, simpleName))
      }
    }
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
                    "buildOther${layout.schemaTypeName(iface.name)}",
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
        .addProperties(fieldDefinitions.propertySpecs())
        .addProperty(typePropertySpec(context.resolver))
        .maybeImplementBuilderFactory(generateDataBuilders, ClassName(packageName, otherBuilderName))
        .build()
  }
}
