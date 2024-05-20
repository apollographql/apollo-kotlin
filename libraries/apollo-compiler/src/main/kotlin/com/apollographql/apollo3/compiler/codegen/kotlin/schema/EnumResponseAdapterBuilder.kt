package com.apollographql.apollo3.compiler.codegen.kotlin.schema

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.requiresOptInAnnotation
import com.apollographql.apollo3.compiler.codegen.responseAdapter
import com.apollographql.apollo3.compiler.codegen.typeAdapterPackageName
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class EnumResponseAdapterBuilder(
    val context: KotlinSchemaContext,
    val enum: IrEnum,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typeAdapterPackageName()
  private val simpleName = layout.schemaTypeName(enum.name).responseAdapter()

  override fun prepare() {
    context.resolver.registerEnumAdapter(
        enum.name,
        ClassName(packageName, simpleName)
    )
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(enum.typeSpec())
    )
  }

  private fun IrEnum.typeSpec(): TypeSpec {
    val adaptedTypeName = context.resolver.resolveSchemaType(enum.name)
    val fromResponseFunSpec = FunSpec.builder(Identifier.fromJson)
        .addAnnotation(requiresOptInAnnotation(KotlinSymbols.ApolloUnknownEnum))
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(Identifier.reader, KotlinSymbols.JsonReader)
        .addParameter(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
        .returns(adaptedTypeName)
        .addCode(
            CodeBlock.builder()
                .addStatement("val·rawValue·=·reader.nextString()!!")
                .addStatement("return·%T.${Identifier.safeValueOf}(rawValue)", adaptedTypeName)
                .build()
        )
        .addModifiers(KModifier.OVERRIDE)
        .build()
    val toResponseFunSpec = toResponseFunSpecBuilder(adaptedTypeName)
        .addCode("${Identifier.writer}.${Identifier.value}(${Identifier.value}.rawValue)")
        .build()

    return TypeSpec.objectBuilder(layout.schemaTypeName(name).responseAdapter())
        .addSuperinterface(KotlinSymbols.Adapter.parameterizedBy(adaptedTypeName))
        .addFunction(fromResponseFunSpec)
        .addFunction(toResponseFunSpec)
        .build()
  }
}

private fun toResponseFunSpecBuilder(typeName: TypeName) = FunSpec.builder(Identifier.toJson)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(name = Identifier.writer, type = KotlinSymbols.JsonWriter)
    .addParameter(Identifier.customScalarAdapters, KotlinSymbols.CustomScalarAdapters)
    .addParameter(Identifier.value, typeName)
