package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.reader
import com.apollographql.apollo3.compiler.codegen.Identifier.safeValueOf
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.value
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.kotlin.CgOutputFileBuilder
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
import com.squareup.kotlinpoet.joinToCode

class EnumResponseAdapterBuilder(
    val context: KotlinContext,
    val enum: IrEnum,
) : CgOutputFileBuilder {
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
    val adaptedTypeName = context.resolver.resolveSchemaType(enum.name)
    val fromResponseFunSpec = FunSpec.builder(Identifier.fromJson)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter(reader, JsonReader::class)
        .addParameter(customScalarAdapters, CustomScalarAdapters::class)
        .returns(adaptedTypeName)
        .addCode(
            CodeBlock.builder()
                .addStatement("val·rawValue·=·reader.nextString()!!")
                .addStatement("return·%T.$safeValueOf(rawValue)", adaptedTypeName)
                .build()
        )
        .addModifiers(KModifier.OVERRIDE)
        .build()
    val toResponseFunSpec = toResponseFunSpecBuilder(adaptedTypeName)
        .addCode("$writer.$value($value.rawValue)")
        .build()

    return TypeSpec
        .objectBuilder(layout.enumResponseAdapterName(name))
        .addSuperinterface(Adapter::class.asClassName().parameterizedBy(adaptedTypeName))
        .addFunction(fromResponseFunSpec)
        .addFunction(toResponseFunSpec)
        .build()
  }
}

private fun toResponseFunSpecBuilder(typeName: TypeName) = FunSpec.builder(toJson)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(name = writer, type = JsonWriter::class.asTypeName())
    .addParameter(name = customScalarAdapters, type = CustomScalarAdapters::class)
    .addParameter(value, typeName)