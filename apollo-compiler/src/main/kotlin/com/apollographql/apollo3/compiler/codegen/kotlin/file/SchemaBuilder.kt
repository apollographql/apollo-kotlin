package com.apollographql.apollo3.compiler.codegen.kotlin.file


import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinClassNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class SchemaBuilder(
    private val context: KotlinContext,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
) : CgOutputFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  override fun prepare() {
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = layout.schemaName(),
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typesPropertySpec(): PropertySpec {
    val allTypenames = interfaces.map { it.name } + objects.map { it.name } + unions.map { it.name }
    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    builder.add(
        allTypenames.map {
          CodeBlock.of("%T.type", context.resolver.resolveSchemaType(it))
        }.joinToString(", ")
    )
    builder.unindent()
    builder.add(")\n")

    return PropertySpec.builder("all", KotlinClassNames.List.parameterizedBy(KotlinClassNames.CompiledType))
        .initializer(builder.build())
        .build()
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(layout.schemaName())
        .addKdoc("A __Schema object containing all the composite types and a possibleTypes helper function")
        .addProperty(typesPropertySpec())
        .addFunction(possibleTypesFunSpec())
        .build()
  }

  private fun possibleTypesFunSpec(): FunSpec {
    val builder = FunSpec.builder("possibleTypes")

    builder.addParameter("type", KotlinClassNames.CompiledNamedType)
    builder.returns(KotlinClassNames.List.parameterizedBy(KotlinClassNames.ObjectType))
    builder.addCode("return %M(all, type)\n", MemberName("com.apollographql.apollo3.api", "possibleTypes"))
    return builder.build()
  }
}