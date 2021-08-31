package com.apollographql.apollo3.compiler.codegen.adapter

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.CgContext
import com.apollographql.apollo3.compiler.codegen.CgFile
import com.apollographql.apollo3.compiler.codegen.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class EnumResponseAdapterBuilder(
    val context: CgContext,
    val enum: IrEnum,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typeAdapterPackageName()
  private val simpleName = layout.enumResponseAdapterName(enum.name)

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
    val adaptedTypeName = context.resolver.resolveEnum(enum.name)
    val fromResponseFunSpec = FunSpec.builder(Identifier.fromJson)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(Identifier.reader, JsonReader::class)
        .addParameter(Identifier.customScalarAdapters, CustomScalarAdapters::class)
        .returns(adaptedTypeName)
        .addCode(
            CodeBlock.builder()
                .addStatement("val rawValue = reader.nextString()!!")
                .addStatement("return %T.valueOf(rawValue)", adaptedTypeName)
                .build()
        )
        .addModifiers(KModifier.OVERRIDE)
        .build()
    val toResponseFunSpec = toResponseFunSpecBuilder(adaptedTypeName)
        .addCode("${Identifier.writer}.${Identifier.value}(${Identifier.value}.rawValue)")
        .build()

    return TypeSpec
        .objectBuilder(layout.enumResponseAdapterName(name))
        .addSuperinterface(Adapter::class.asClassName().parameterizedBy(adaptedTypeName))
        .addFunction(fromResponseFunSpec)
        .addFunction(toResponseFunSpec)
        .build()
  }
}

internal fun toResponseFunSpecBuilder(typeName: TypeName) = FunSpec.builder(toJson)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(name = writer, type = JsonWriter::class.asTypeName())
    .addParameter(name = customScalarAdapters, type = CustomScalarAdapters::class)
    .addParameter(value, typeName)
