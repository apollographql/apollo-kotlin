package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols.CompiledArgumentDefinitionBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.concreteBuilderTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.concreteMapTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeImplementBuilderFactory
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.topLevelBuildFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrArgumentDefinition
import com.apollographql.apollo.compiler.ir.IrFieldDefinition
import com.apollographql.apollo.compiler.ir.IrObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class ObjectBuilder(
    private val context: KotlinSchemaContext,
    private val obj: IrObject,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(obj.name)
  private val builderName = "${layout.schemaTypeName(obj.name)}Builder"
  private val mapName = "${layout.schemaTypeName(obj.name)}Map"

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName(packageName, simpleName))
    context.resolver.registerMapType(obj.name, ClassName(packageName, mapName))
    context.resolver.registerBuilderType(obj.name, ClassName(packageName, builderName))
    context.resolver.registerBuilderFun(obj.name, MemberName(packageName, "build${layout.schemaTypeName(obj.name)}"))
    for (fieldDefinition in obj.fieldDefinitions) {
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
        "build${layout.schemaTypeName(name)}",
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
        .addProperties(fieldDefinitions.propertySpecs())
        .addProperty(typePropertySpec(context.resolver))
        .maybeImplementBuilderFactory(generateDataBuilders, context.resolver.resolveBuilderType(name))
        .build()
  }
}

internal fun List<IrFieldDefinition>.propertySpecs(): List<PropertySpec> {
  return flatMap { fieldDefinition ->
    fieldDefinition.argumentDefinitions.map { argumentDefinition ->
      PropertySpec.builder(
          name = argumentDefinition.propertyName,
          type = KotlinSymbols.CompiledArgumentDefinition,
      )
          .initializer(argumentDefinition.codeBlock())
          .build()
    }
  }
}

private fun IrArgumentDefinition.codeBlock(): CodeBlock {
  val argumentBuilder = CodeBlock.builder()
  argumentBuilder.add(
      "%T(%S)",
      CompiledArgumentDefinitionBuilder,
      name,
  )

  if (isKey) {
    argumentBuilder.add(".isKey(true)")
  }
  if (isPagination) {
    argumentBuilder.add(".isPagination(true)")
  }
  argumentBuilder.add(".build()")
  return argumentBuilder.build()
}
