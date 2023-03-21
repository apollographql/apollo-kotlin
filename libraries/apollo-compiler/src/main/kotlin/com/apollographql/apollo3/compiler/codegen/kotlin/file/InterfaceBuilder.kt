package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

internal class InterfaceBuilder(
    private val context: KotlinContext,
    private val iface: IrInterface,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(iface.name)
  private val builderName = layout.builderName(iface.name)
  private val otherBuilderName = layout.otherBuilderName(iface.name)
  private val mapName = layout.mapName(iface.name)
  private val otherMapName = layout.otherMapName(iface.name)

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
                    layout.buildOtherFunName(iface.name),
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
