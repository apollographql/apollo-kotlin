package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.NamedType
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.builderTypeSpec
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.makeClassFromParameters
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrInputObject
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeSpec

internal class InputObjectBuilder(
    val context: KotlinSchemaContext,
    val inputObject: IrInputObject,
    val generateInputBuilders: Boolean,
    val withDefaultArguments: Boolean,
) : CgFileBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.schemaTypeName(inputObject.name)
  private val className = ClassName(packageName, simpleName)

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(inputObject.typeSpec())
    )
  }

  override fun prepare() {
    context.resolver.registerSchemaType(
        inputObject.name,
        className
    )
  }

  private fun IrInputObject.typeSpec(): TypeSpec {
    val namedTypes = fields.map {
      it.toNamedType()
    }
    return TypeSpec
        .classBuilder(simpleName)
        .maybeAddDescription(description)
        .makeClassFromParameters(
            context.generateMethods,
            namedTypes.map { it.toParameterSpec(context, withDefaultArguments) },
            className = context.resolver.resolveSchemaType(inputObject.name)
        )
        .apply {
          if (namedTypes.isNotEmpty() && generateInputBuilders) {
            addType(namedTypes.builderTypeSpec(context, className))
          }
        }
        .apply {
          if (isOneOf) {
            addInitializerBlock(namedTypes.oneOfInitializerBlock(context))
          }
        }
        .build()
  }
}

private fun List<NamedType>.oneOfInitializerBlock(context: KotlinSchemaContext): CodeBlock {
  return CodeBlock.of("%M(${joinToString { context.layout.propertyName(it.graphQlName) }})\n", KotlinSymbols.assertOneOf)
}
