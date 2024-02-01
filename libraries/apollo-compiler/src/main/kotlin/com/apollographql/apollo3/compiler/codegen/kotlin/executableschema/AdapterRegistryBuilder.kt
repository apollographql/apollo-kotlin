package com.apollographql.apollo3.compiler.codegen.kotlin.executableschema

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.executionPackageName
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinExecutableSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.nullable
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec

internal class AdapterRegistryBuilder(
    val context: KotlinExecutableSchemaContext,
    val serviceName: String,
    val codegenSchema: CodegenSchema
) : CgFileBuilder {
  private val packageName = context.layout.executionPackageName()
  private val simpleName = "${serviceName}AdapterRegistry".capitalizeFirstLetter()

  val memberName = MemberName(packageName, simpleName)

  override fun prepare() {
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        propertySpecs = listOf(propertySpec()),
        fileName = simpleName
    )
  }

  private fun propertySpec(): PropertySpec {
    return PropertySpec.builder(simpleName, KotlinSymbols.CustomScalarAdapters)
        .initializer(initializerCode())
        .build()
  }

  private fun initializerCode(): CodeBlock {
    return CodeBlock.builder()
        .add("%T()\n", KotlinSymbols.CustomScalarAdaptersBuilder)
        .apply {
          codegenSchema.schema.typeDefinitions.values.forEach {
            if (it.name in setOf("String", "Float", "Boolean", "ID", "Int") || it.name.startsWith("__")) {
              return@forEach
            }

            when (it) {
              is GQLObjectTypeDefinition,
              is GQLInterfaceTypeDefinition,
              is GQLUnionTypeDefinition
              -> {
                // Those require a context and can't use simple Adapter
                return@forEach
              }

              else -> {
                val type = when (it) {
                  is GQLEnumTypeDefinition -> IrEnumType(it.name, nullable = true)
                  is GQLInputObjectTypeDefinition -> IrInputObjectType(it.name, nullable = true)
                  is GQLScalarTypeDefinition -> IrScalarType(it.name, nullable = true)
                  else -> error("")
                }
                add(".add(%S,·%L)\n", it.name, context.resolver.adapterInitializer(type.nullable(false), false, false, ""))
              }
            }
          }
        }
        .add(".build()\n")
        .build()
  }
}