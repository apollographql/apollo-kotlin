package com.apollographql.apollo3.compiler.codegen.kotlin.schema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.concreteBuilderTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.concreteMapTypeSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeImplementBuilderFactory
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.topLevelBuildFunSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo3.compiler.codegen.typePackageName
import com.apollographql.apollo3.compiler.ir.IrObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec

internal class ObjectBuilder(
    private val context: KotlinSchemaContext,
    private val obj: IrObject,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(obj.name)
  private val builderName = "${obj.name.capitalizeFirstLetter()}Builder"
  private val mapName = "${obj.name.capitalizeFirstLetter()}Map"

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(obj.name, ClassName(packageName, mapName))
    context.resolver.registerBuilderType(obj.name, ClassName(packageName, builderName))
    context.resolver.registerBuilderFun(obj.name, MemberName(packageName, "build${obj.name.capitalizeFirstLetter()}"))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = mutableListOf<TypeSpec>().apply {
          add(obj.typeSpec())
          if (generateDataBuilders) {
            add(concreteBuilderTypeSpec(context, packageName = packageName, builderName = builderName, mapName = mapName, obj.mapProperties, obj.name))
            add(obj.mapTypeSpec())
          }
        },
        funSpecs = mutableListOf<FunSpec>().apply {
          if (generateDataBuilders) {
            add(obj.builderFunSpec())
          }
        }

    )
  }

  private fun IrObject.mapTypeSpec(): TypeSpec {
    return concreteMapTypeSpec(resolver = context.resolver, mapName = mapName, extendsFromType = null, implements = superTypes)
  }

  private fun IrObject.builderFunSpec(): FunSpec {
    return topLevelBuildFunSpec(
        "build${name.capitalizeFirstLetter()}",
        ClassName(packageName, builderName),
        ClassName(packageName, mapName),
        requiresTypename = false
    )
  }


  private fun IrObject.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrObject.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .maybeImplementBuilderFactory(generateDataBuilders, context.resolver.resolveBuilderType(name))
        .build()
  }
}
