package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.InputResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.lang.IllegalStateException

internal fun CodeGenerationAst.FieldType.asInputTypeName() = if (nullable) {
  Input::class.asClassName().parameterizedBy(asTypeName().copy(nullable = false))
} else {
  asTypeName()
}

internal fun CodeGenerationAst.FieldType.asInputAdapterTypeName(): TypeName {
  return ResponseAdapter::class.asClassName().parameterizedBy(asInputTypeName())
}

internal fun CodeGenerationAst.InputField.toParameterSpec(): ParameterSpec {
  return ParameterSpec
      .builder(
          name = name.escapeKotlinReservedWord(),
          type = type.asTypeName().let { type ->
            if (type.isNullable) Input::class.asClassName().parameterizedBy(type.copy(nullable = false)) else type
          }
      )
      .applyIf(description.isNotBlank()) { addKdoc("%L\n", description) }
      .applyIf(type.nullable) { defaultValue("%T()", Input.Absent::class.asClassName()) }
      .build()
}

fun serializeVariablesFunSpec(
    funName: String,
    packageName: String,
    name: String,
    getOrPut: String): FunSpec {
  val serializerClassName = ClassName("$packageName.adapter", kotlinNameForSerializer(name))
  val body = CodeBlock.builder().apply {
    addStatement("${Identifier.RESPONSE_ADAPTER_CACHE}.$getOrPut {")
    indent()
    addStatement("%T(${Identifier.RESPONSE_ADAPTER_CACHE})", serializerClassName)
    unindent()
    addStatement("}.toResponse(writer, this)")
  }.build()

  return FunSpec.builder(funName)
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("writer", JsonWriter::class)
      .addParameter(ParameterSpec.builder(Identifier.RESPONSE_ADAPTER_CACHE, ResponseAdapterCache::class.asTypeName()).build())
      .addCode(body)
      .build()

}

fun notImplementedFromResponseFunSpec(returnTypeName: TypeName) = FunSpec.builder("fromResponse")
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec.builder(Identifier.READER, JsonReader::class).build())
    .returns(returnTypeName)
    .addCode("throw %T(%S)", IllegalStateException::class, "Input type used in output position")
    .build()

internal fun List<CodeGenerationAst.InputField>.serializerTypeSpec(
    packageName: String,
    name: String
): TypeSpec {
  val className = ClassName(packageName, name)
  val builder = TypeSpec.classBuilder(kotlinNameForSerializer(name))

  builder.addSuperinterface(ResponseAdapter::class.asClassName().parameterizedBy(className))

  builder.primaryConstructor(FunSpec.constructorBuilder()
      .addParameter(Identifier.RESPONSE_ADAPTER_CACHE, ResponseAdapterCache::class)
      .build()
  )

  forEach {
    builder.addProperty(it.adapterPropertySpec())
  }

  builder.addFunction(notImplementedFromResponseFunSpec(className))
  builder.addFunction(FunSpec.builder(Identifier.TO_RESPONSE)
      .addParameter(Identifier.WRITER, JsonWriter::class)
      .addParameter(Identifier.VALUE, className)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(CodeBlock.Builder().apply {
        forEach {
          addStatement("%L.toResponse(writer, value.%L)",
              kotlinNameForVariableAdapterField(it.name, it.type),
              kotlinNameForVariable(it.name)
          )
        }
      }.build())
      .build()
  )

  return builder.build()
}

private fun CodeGenerationAst.InputField.adapterPropertySpec(): PropertySpec {
  val initializer = if (type.nullable) {
    CodeBlock.of("%T(%S, %L)", InputResponseAdapter::class, schemaName, adapterInitializer(type.nonNullable()))
  } else {
    adapterInitializer(type)
  }

  return PropertySpec.builder(kotlinNameForVariableAdapterField(name, type), type.asInputAdapterTypeName())
      .initializer(initializer)
      .build()
}