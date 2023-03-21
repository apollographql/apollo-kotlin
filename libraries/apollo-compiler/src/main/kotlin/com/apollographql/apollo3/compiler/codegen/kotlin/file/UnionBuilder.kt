package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

internal class UnionBuilder(
    private val context: KotlinContext,
    private val union: IrUnion,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(union.name)
  private val builderName = layout.builderName(union.name)
  private val otherBuilderName = layout.otherBuilderName(union.name)
  private val mapName = layout.mapName(union.name)
  private val otherMapName = layout.otherMapName(union.name)

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
                    layout.buildOtherFunName(union.name),
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
