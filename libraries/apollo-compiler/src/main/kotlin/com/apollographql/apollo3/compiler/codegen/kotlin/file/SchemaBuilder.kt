package com.apollographql.apollo3.compiler.codegen.kotlin.file


import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class SchemaBuilder(
    private val context: KotlinContext,
    private val generatedSchemaName: String,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
    private val enums: List<IrEnum>
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.schemaPackageName()

  override fun prepare() {
    context.resolver.registerSchema(ClassName(packageName, generatedSchemaName))
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = generatedSchemaName,
        typeSpecs = listOf(typeSpec())
    )
  }

  private fun typesPropertySpec(): PropertySpec {
    val allTypenames = interfaces.map { it.name } + objects.map { it.name } + unions.map { it.name } + enums.map { it.name }
    val builder = CodeBlock.builder()
    builder.add("listOf(\n")
    builder.indent()
    builder.add(
        allTypenames.sortedBy { it }.map {
          CodeBlock.of("%T.$type", context.resolver.resolveSchemaType(it))
        }.joinToString(", ")
    )
    builder.unindent()
    builder.add(")\n")

    return PropertySpec.builder("all", KotlinSymbols.List.parameterizedBy(KotlinSymbols.CompiledNamedType))
        .initializer(builder.build())
        .build()
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.objectBuilder(generatedSchemaName)
        .addKdoc("A __Schema object containing all the composite types and a possibleTypes helper function")
        .addProperty(typesPropertySpec())
        .addFunction(possibleTypesFunSpec())
        .build()
  }

  private fun possibleTypesFunSpec(): FunSpec {
    val builder = FunSpec.builder("possibleTypes")

    builder.addParameter("type", KotlinSymbols.CompiledNamedType)
    builder.returns(KotlinSymbols.List.parameterizedBy(KotlinSymbols.ObjectType))
    builder.addCode("return·%M(all, type)\n", MemberName("com.apollographql.apollo3.api", "possibleTypes"))
    return builder.build()
  }
}
