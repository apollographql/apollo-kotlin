package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrEnumType
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec

internal class AdapterRegistryBuilder(
    val context: KotlinContext,
    val serviceName: String,
    val codegenSchema: CodegenSchema
) : CgFileBuilder {
  private val packageName = context.layout.executionPackageName()
  private val simpleName = context.layout.capitalizedIdentifier("${serviceName}AdapterRegistry")

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
              is GQLUnionTypeDefinition -> {
                // Those require a context and can't use simple Adapter
                return@forEach
              }
              else -> {
                val type = when (it) {
                  is GQLEnumTypeDefinition -> IrEnumType(it.name)
                  is GQLInputObjectTypeDefinition -> IrInputObjectType(it.name)
                  is GQLScalarTypeDefinition -> IrScalarType(it.name)
                  else -> error("")
                }
                add(".add(%S,·%L)\n", it.name, context.resolver.adapterInitializer(IrNonNullType(type), false, false, ""))
              } }
          }
        }
        .add(".build()\n")
        .build()
  }
}
