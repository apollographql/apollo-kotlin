package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols.CompiledArgumentDefinitionBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.dataTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.schema.util.typePropertySpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrArgumentDefinition
import com.apollographql.apollo.compiler.ir.IrFieldDefinition
import com.apollographql.apollo.compiler.ir.IrObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class ObjectBuilder(
    private val context: KotlinSchemaContext,
    private val obj: IrObject,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(obj.name)

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName(packageName, simpleName))
    for (fieldDefinition in obj.fieldDefinitions) {
      fieldDefinition.argumentDefinitions.forEach { argumentDefinition ->
        context.resolver.registerArgumentDefinition(argumentDefinition.id, ClassName(packageName, simpleName, argumentsHolder))
      }
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(obj.typeSpec()),
    )
  }

  private fun IrObject.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addType(dataTypeSpec())
        .maybeAddArguments(fieldDefinitions)
        .addType(companionTypeSpec())
        .build()
  }

  private fun IrObject.companionTypeSpec(): TypeSpec {
    return TypeSpec.companionObjectBuilder()
        .addProperty(typePropertySpec(context.resolver))
        .build()
  }
}

internal const val argumentsHolder = "__Arguments"

/**
 * The argument definitions go in their own object rather than in the companion object next to `type`. The two
 * are independent - `type` is read when building selections and cache keys, the argument definitions only by
 * the selections of fields that take arguments - but sharing a class initializer means reading either one
 * builds both. Schema types with many arguments are the expensive case: reading `type` would build every
 * [KotlinSymbols.CompiledArgumentDefinition] of the type along with it.
 */
internal fun TypeSpec.Builder.maybeAddArguments(fieldDefinitions: List<IrFieldDefinition>) = apply {
  val propertySpecs = fieldDefinitions.flatMap { fieldDefinition ->
    fieldDefinition.argumentDefinitions.map { argumentDefinition ->
      PropertySpec.builder(
          name = argumentDefinition.propertyName,
          type = KotlinSymbols.CompiledArgumentDefinition,
      )
          .initializer(argumentDefinition.codeBlock())
          .build()
    }
  }

  if (propertySpecs.isNotEmpty()) {
    addType(
        TypeSpec.objectBuilder(argumentsHolder)
            .addProperties(propertySpecs)
            .build()
    )
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
  argumentBuilder.add(".build()")
  return argumentBuilder.build()
}


